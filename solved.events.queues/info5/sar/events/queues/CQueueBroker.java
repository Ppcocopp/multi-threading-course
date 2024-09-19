/*
 * Copyright (C) 2023 Pr. Olivier Gruber                                    
 *                                                                       
 * This program is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU General Public License as published by  
 * the Free Software Foundation, either version 3 of the License, or     
 * (at your option) any later version.                                   
 *                                                                       
 * This program is distributed in the hope that it will be useful,       
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         
 * GNU General Public License for more details.                          
 *                                                                       
 * You should have received a copy of the GNU General Public License     
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package info5.sar.events.queues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info5.sar.events.channels.Broker;
import info5.sar.events.channels.Channel;
import info5.sar.events.queues.events.QueueBroker;
import info5.sar.utils.Executor;

/**
 * This is for the full event-oriented implementation, using a single event pump
 * (Executor). You must specify, design, and code the event-oriented version of
 * the channels and their brokers.
 */
public class CQueueBroker extends QueueBroker {

	// map of the binded ports and their objects to synchronize on
	private Map<Integer, Object> binds = new HashMap<>();
	// list of unbinding ports
	private List<Integer> unbinds = new ArrayList<>();

	public CQueueBroker(Executor pump, Broker broker) {
		super(pump, broker);
	}

	@Override
	public synchronized boolean bind(int port, AcceptListener listener) {
		if(binds.containsKey(port)) return false;
		binds.put(port, new Object());
		
		Broker.AcceptListener acceptListener = new Broker.AcceptListener() {
			@Override
			public void accepted(Channel channel) {
				CMessageQueue messageQueue = new CMessageQueue(channel, self(), getEventPump());
				Channel.ReadListener readListener = new Channel.ReadListener() {
					@Override
					public void read(byte[] bytes) {
						if(bytes.equals(CMessageQueue.UNBINDING_QUEUE_PAYLOAD)) {
							messageQueue.close();
							unbinds.add(port);
						}else {
							Runnable r = new Runnable() {
								@Override
								public void run() {
									listener.accepted(messageQueue);						
								}
							};
							getEventPump().post(r);
							if(binds.containsKey(port) || !unbinds.contains(port))
								getBroker().accept(port, acceptListener());
						}
					}
				};
				messageQueue.startReadPayload(readListener);
			}
			
			private Broker.AcceptListener acceptListener() {return this;}
		};
		getBroker().accept(port, acceptListener);
		unbinds.remove(Integer.valueOf(port));
		return true;
	}

	@Override
	public boolean unbind(int port) {
		synchronized(binds.get(Integer.valueOf(port))==null ? new Object() : binds.get(Integer.valueOf(port))) {
			if(binds.remove(Integer.valueOf(port))==null) return false;
			Broker.ConnectListener connectListener = new Broker.ConnectListener() {
				@Override
				public void connected(Channel channel) {
					CMessageQueue queue = new CMessageQueue(channel, self(), self().getEventPump());
					queue.send(CMessageQueue.UNBINDING_QUEUE_PAYLOAD);
				}
			};
			
			if(!getBroker().connect(this.getName(), port, connectListener)) {
				return false;
			}
			return true;
		}
	}

	@Override
	public boolean connect(String name, int port, ConnectListener listener) {
		Broker.ConnectListener connectListener = new Broker.ConnectListener() {
			@Override
			public void connected(Channel channel) {
				CMessageQueue queue = new CMessageQueue(channel, self(), self().getEventPump());
				queue.send(CMessageQueue.REGULAR_QUEUE_PAYLOAD);
				Runnable r = new Runnable() {
					@Override
					public void run() {
						listener.connected(queue);
					}
				};
				getEventPump().post(r);
			}
		};
		
		if(!getBroker().connect(name, port, connectListener)) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					listener.refused();
				}
			};
			this.getEventPump().post(r);
			return false;
		}
		return true;
	}
	
	/**
	 * @return yourself
	 */
	private CQueueBroker self() {
		return this;
	}

}
