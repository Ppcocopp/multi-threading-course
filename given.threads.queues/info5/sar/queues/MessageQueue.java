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

/**
 * MessageQueue are there to permit to send and receive byte arrays called messages.<br>
 * <br>
 * MessageQueues are layouts of {@link info5.sar.channels.Channel Channels}.
 */
public abstract class MessageQueue {
	
	/**
	 * @return The parent QueueBroker
	 */
	abstract public QueueBroker broker();

	/**
	 * Sends a 'length' long message contained in the 'bytes' array starting at the 'offset' index.<br>
	 * This is a thread-safe blocking FIFO method.
	 * <pre> Example : send([0,1,2,3,4,5,6,7,8,9], 2, 4) will send : [2, 3, 4, 5] </pre>
	 * @param bytes : the array containing the message to send
	 * @param offset : the starting index of the message in the array
	 * @param length : the length of the message
	 * @throws ClosedException if the connection is closed
	 */
	abstract public void send(byte[] bytes, int offset, int length) throws ClosedException;

	/**
	 * This is a thread-safe blocking FIFO method.
	 * @return A single received message
	 * @throws ClosedException if the connection is closed
	 */
	abstract public byte[] receive() throws ClosedException;

	/**
	 * Thread-safe closes this MessageQueue, unblocking any thread 
	 * blocked in send() or receive() operation.
	 */
	abstract public void close();

	/**
	 * @returns true if this MessageQueue is closed (thread-safe)
	 */
	abstract public boolean closed();
}
