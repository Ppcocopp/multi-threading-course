package info5.sar.mixed.queues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info5.sar.channels.Broker;
import info5.sar.channels.Channel;
import info5.sar.events.queues.mixed.MessageQueue;
import info5.sar.events.queues.mixed.QueueBroker;
import info5.sar.utils.Executor;


public class CQueueBroker extends QueueBroker {
	
	// map of the binded ports and their objects to synchronize on
	private Map<Integer, Object> binds = new HashMap<>();
	// list of unbinding ports
	private List<Integer> unbinds = new ArrayList<>();
	
	public CQueueBroker(Executor executor, Broker broker) {
		super(executor, broker);
	}

	/*
	 * If the binds field contains the port key, return false.
	 * Add the given port as key to the binds field with a new object as value.
	 * Create a new worker Thread doing in the run() method :
	 * - while the binds field contains the port as key or the unbinds list doesn't contain the port :
	 * - - Get the channel from the accept() of the broker on the given port.
	 * - - Create a new CMessageQueue from the channel.
	 * - - If the readPayload() of the MessageQueue equals the unbinding queue payload constant of CMessageQueue :
	 * - - - close the MessageQueue and add the port to the unbinds field.
	 * - - Else :
	 * - - - Create a new Runnable which calls the accepted() method of the listener with the new MessageQueue
	 * - - - Post this Runnable to the event executor pump
	 * - Remove the port from the unbinds field.
	 * Start the thread.
	 * Return true.
	 */
	@Override
	public synchronized boolean bind(int port, AcceptListener listener) {
		if(binds.containsKey(port)) return false;
		binds.put(port, new Object());
		Thread worker = new Thread() {			
			@Override
			public void run() {
				while(binds.containsKey(port) || !unbinds.contains(port)) {
					Channel channel = getBroker().accept(port);
					CMessageQueue messageQueue = new CMessageQueue(channel, self(), getEventPump());
					if(messageQueue.readPayload().equals(CMessageQueue.UNBINDING_QUEUE_PAYLOAD)) {
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
					}
				}
				unbinds.remove(port);
			}
		};
		worker.start();
		return true;
	}

	/**
	 * In a synchronized block on the object associated to the given port on the binds field, a new object if the port is not a key of this field :
	 * | if the returned value when remove() on the binds field on the given port is null : return false
	 * | Create a new ConnectListener.
	 * | The connected() method should send the unbinding queue payload from the given queue.
	 * | The refused() method should do nothing.
	 * | Return the returned value when connect() on the given port with the new listener.
	 */
	@Override
	public boolean unbind(int port) {
		synchronized(binds.get(Integer.valueOf(port))==null ? new Object() : binds.get(Integer.valueOf(port))) {
			if(binds.remove(Integer.valueOf(port))==null) return false;
			ConnectListener listener = new ConnectListener() {
				@Override
				public void connected(MessageQueue queue) {
					queue.send(CMessageQueue.UNBINDING_QUEUE_PAYLOAD);
				}

				@Override
				public void refused() {
					// never happens
				}
			};
			return connect(this.getName(), port, listener);
		}
	}

	/*
	 * Create a new worker Thread doing in the run() method :
	 * - retrieve the returned channel of connect() on the broker
	 * - if the channel is null :
	 * - - create a new Runnable which calls the refused() method from the listener in its run() method
	 * - - post the new Runnable to the event executor pump
	 * - else :
	 * - - create a new CMessageQueue from this channel
	 * - - send the regular queue payload from this messageQueue
	 * - - create a new Runnable which calls the connected() method of the listener from its run() method.
	 * - - post this Runnable to the event executor pump
	 * Start the thread
	 * Return true
	 */
	@Override
	public synchronized boolean connect(String name, int port, ConnectListener listener) {
		Thread worker = new Thread() {
			@Override
			public void run() {
				Channel channel = getBroker().connect(name, port);
				if(channel==null) {
					Runnable r = new Runnable() {
						@Override
						public void run() {
							listener.refused();
						}
					};
					getEventPump().post(r);
				}else {
					CMessageQueue messageQueue = new CMessageQueue(channel, self(), getEventPump());
					messageQueue.send(CMessageQueue.REGULAR_QUEUE_PAYLOAD);
					Runnable r = new Runnable() {
						@Override
						public void run() {
							listener.connected(messageQueue);
						}
					};
					getEventPump().post(r);
				}
			}
		};
		worker.start();
		return true;
	}
	
	/**
	 * @return yourself
	 */
	private CQueueBroker self() {
		return this;
	}

}
