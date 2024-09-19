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
package info5.sar.events.queues.mixed;

/**
 * MessageQueue are there to permit to send and receive byte arrays called messages.<br>
 * <br>
 * MessageQueues are layouts of {@link info5.sar.channels.Channel Channels}.<br>
 * <br>
 * This won't receive any messages until the {@link #setListener(Listener) setListener()} method is called.
 */
public abstract class MessageQueue {

	/**
	 * @return The parent QueueBroker
	 */
	abstract public QueueBroker broker();

	/**
	 * Added to help debug applications
	 * @return The name of the remote MessageQueue
	 */
	public abstract String getRemoteName();

	/**
	 * Listener for MessageQueue.
	 * Allows to define the behavior when a message is received and when the communication is closed.
	 */
	public interface Listener {
		/**
		 * Defines the behavior when a message is received.
		 * @param msg : the received message
		 */
		void received(byte[] msg);

		/**
		 * Defines the behavior when the communication is closed.
		 */
		void closed();
	}

	/**
	 * Allows to set the listener to use at message receiving and connection closing.
	 * Also starts the automatic reception of messages if called for the first time.
	 * This is a thread-safe non-blocking FIFO method.
	 * @param l : the new listener to use
	 */
	public abstract void setListener(Listener l);

	/**
	 * This method avoids any ownership issue on the array so the array can be changed at the return of this method().
	 * This is a thread-safe non-blocking FIFO method.
	 * @param bytes : the message to send
	 * @return True if the message is successfully queued for sending
	 */
	public abstract boolean send(byte[] bytes);

	/**
	 * Thread-safe closes this MessageQueue, unblocking any thread 
	 * blocked in send() or receive() operation.
	 */
	public abstract void close();

	/**
	 * @returns true if this MessageQueue is closed (thread-safe)
	 */
	public abstract boolean closed();
}
