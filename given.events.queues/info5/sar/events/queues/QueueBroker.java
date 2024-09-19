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

import info5.sar.channels.Broker;
import info5.sar.utils.Executor;

/**
 * QueueBroker are there to permit to establish MessageQueues.<br>
 * <br>
 * QueueBrokers are layouts of {@link Broker Brokers}.
 */
public abstract class QueueBroker {
	
	// Broker to layout
	private Broker broker;
	// the event executor pump
	private Executor pump;

	/*
	 * Set the broker field with the given broker.
	 * Set the pump field with the given pump.
	 */
	public QueueBroker(Executor pump, Broker broker) {
		this.broker = broker;
		this.pump = pump;
	}

	/*
	 * Return the pump field
	 */
	/**
	 * @return The event executor pump
	 */
	public Executor getEventPump() {
		return pump;
	}

	/*
	 * Return the value of getName() of the broker field
	 */
	/**
	 * @return the name of the broker
	 */
	public String getName() {
		return broker.getName();
	}

	/*
	 * Return the field broker
	 */
	/**
	 * @return the used broker
	 */
	public Broker getBroker() {
		return broker;
	}

	/**
	 * Listener for QueueBroker {@link QueueBroker#bind(int,AcceptListener) bind()} method.<br>
	 * Allows to define the behavior when a connection is accepted and fully-connected.
	 */
	public interface AcceptListener {
		/**
		 * Defines the behavior when a connection is accepted and fully connected
		 * @param queue : the resulting MessageQueue of the connection
		 */
		void accepted(MessageQueue queue);
	}

	/**
	 * Allows to accept any connection on the given port until the {@link #unbind(int)} method is called.<br>
	 * Sets the listener to use on this port for when a connection is accepted and fully-connected.<br>
	 * This will do nothing if the given port is already binded.
	 * This is a thread-safe non-blocking method.
	 * @param port : the communication port to accept on
	 * @param listener : the listener to use
	 * @return True if the given port is not already binded, false otherwise
	 */
	public abstract boolean bind(int port, AcceptListener listener);

	/**
	 * Allows to stop a binded port.
	 * This won't close previous connections on this port.
	 * This is a thread-safe non-blocking method.
	 * @param port : the communication port
	 * @return True if the port is successfully unbinded, false otherwise
	 */
	public abstract boolean unbind(int port);

	/**
	 * Listener for QueueBroker {@link QueueBroker#connect(String,int,ConnectListener) connect()} method.<br>
	 * Allows to define the behavior when a connection is fully-connected or refused.
	 */
	public interface ConnectListener {
		/**
		 * Defines the behavior when a connection is fully-connected.
		 * @param queue : the resulting MessageQueue of the connection
		 */
		void connected(MessageQueue queue);

		/**
		 * Defines the behavior when the connection is refused.
		 */
		void refused();
	}

	/**
	 * Allows to connect to another QueueBroker on the given port.
	 * Sets the listener to use when the connection is fully-connected or if refused.
	 * The connection is refused if the given name leads to no QueueBroker.
	 * This is a thread-safe non-blocking method.
	 * @param name : name of the remote QueueBroker
	 * @param port : the communication port
	 * @param listener : the listener to use
	 * @return True
	 */
	public abstract boolean connect(String name, int port, ConnectListener listener);

}
