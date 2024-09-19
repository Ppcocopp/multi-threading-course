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
package info5.sar.queues;

import info5.sar.channels.Broker;

/**
 * QueueBroker are there to permit to establish MessageQueues.<br>
 * <br>
 * QueueBrokers are layouts of {@link Broker Brokers}.
 */
public abstract class QueueBroker {

	// Broker to layout
	protected Broker broker;

	/*
	 * Set the broker field with the given broker
	 */
	/**
	 * @param broker : the Broker to layout
	 */
	public QueueBroker(Broker broker) {
		this.broker = broker;
	}

	/*
	 * Return the value of getName() of the broker in field
	 */
	/**
	 * @return The name of the Broker
	 */
	public String getName() {
		return broker.getName();
	}

	/*
	 * Return the broker in field
	 */
	/**
	 * @return The used Broker
	 */
	public Broker getBroker() {
		return broker;
	}

	/**
	 * Indicate that this QueueBroker will accept one connection on the given port and
	 * return a fully connected MessageQueue. This is a thread-safe blocking rendez-vous.
	 * 
	 * @param port : the connection port
	 * @throws IllegalArgumentException if there is already an accept pending on the
	 *                                  given port.
	 */
	abstract public MessageQueue accept(int port);

	/**
	 * Attempts a connection to the given port, via the QueueBroker with the given name.
	 * If such a QueueBroker cannot be found, this method returns null. If the QueueBroker is
	 * found, this connect will block until an accept on the given port is pending.
	 * This is a thread-safe blocking rendez-vous. Note: multiple accepts from
	 * different tasks with the same name and port are legal
	 * 
	 * @param name : name of the QueueBroker to connect to.
	 * @return The resulting fully connected MessageQueue or null if no QueueBroker with the
	 *         given name has been found.
	 */
	abstract public MessageQueue connect(String name, int port);

}
