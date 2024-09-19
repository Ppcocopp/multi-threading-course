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
package info5.sar.events.channels;

import info5.sar.channels.DisconnectedException;

/**
 * Channel is a point-to-point stream of bytes.
 * Full-duplex, each end point can be used to read or write.
 * A connected channel is FIFO and lossless. 
 * A channel can be disconnected at any time, from either side.
 */
public abstract class Channel {
  Broker broker;

  /**
   * @param broker : the parent Broker
   * @param executor : the executor event pump
   */
  protected Channel(Broker broker) {
    this.broker = broker;
  }

  /**
   * Added for helping debugging applications.
   * @return The name of the remote Broker
   */
  public abstract String getRemoteName();
  
  /**
   * @return The parent Broker
   */
  public Broker getBroker() {
    return broker;
  }
  
  /**
   * @return The connection port
   */
  public abstract int getPort();
  
  /**
   * Listener for Channel
   * Allows to define the behavior when a byte array has been read.
   */
  public interface ReadListener {
	  /**
	   * Defines the behavior when a byte array has been read.
	   * @param bytes : the read bytes
	   */
	  public void read(byte[] bytes);
  }

  /**
   * Read bytes in the given array, starting at the given offset.<br>
   * At most "length" bytes will be read.<br>
   * This method will read at least 1 bytes.<br>
   * This is a thread-safe non-blocking FIFO method.
   *  
   * @param bytes : the array to write on
   * @param offset : the starting index in the array
   * @param length : number of bytes to read
   * @param listener : the listener to call when finished
   * @throws DisconnectedException if this Channel is disconnected.
   */
  public abstract void read(byte[] bytes, int offset, int length, ReadListener listener) throws DisconnectedException;
  
  /**
   * Listener for Channel
   * Allows to define the behavior when a byte array has been written.
   */
  public interface WriteListener {
	  /**
	   * Defines the behavior when a byte array has been written.
	   * @param bytes : the written bytes
	   * @param offset : the starting index
	   * @param length : number of bytes written
	   */
	  public void written(byte[] bytes, int offset, int length, int written);
  }
  
  /**
   * Write bytes from the given array, starting at the given offset.<br>
   * At most "length" bytes will be written.<br>
   * This method will write at least 1 bytes.<br>
   * This is a thread-safe non-blocking FIFO method.
   * 
   * @param bytes : the array to read from
   * @param offset : the starting index in the array
   * @param length : number of bytes to write
   * @param listener : listener to call when finished
   * @throws DisconnectedException if this Channel is disconnected.
   */
  public abstract void write(byte[] bytes, int offset, int length, WriteListener listener) throws DisconnectedException;

  /**
   * Thread-safe disconnects this Channel, unblocking any thread 
   * blocked read or write operation.
   */
  public abstract void disconnect();

  /**
   * @returns true if this Channel is disconnected (thread-safe)
   */
  public abstract boolean disconnected();

}
