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
package info5.sar.channels;

/**
 * Channel is a point-to-point stream of bytes.
 * Full-duplex, each end point can be used to read or write.
 * A connected Channel is FIFO and lossless. 
 * A Channel can be disconnected at any time, from either side.
 * 
 * Overall, this class should not be considered thread safe 
 * since the read and write operations may complete partially.
 * However, it is safe to use one thread to read and one thread 
 * to write. It is also safe to use different threads to operate
 * sequentially on a Channel, like a message layer sending or 
 * receiving messages through, using different threads. 
 * Note this is correct only if the needed synchronization
 * to ensure the atomicity of the send or receive operations 
 * is handled properly at the message layer.
 * 
 * It is safe to disconnect a Channel from any thread. This means
 * that a channel can be disconnect from one thread while other
 * threads are blocked in a read or write operation. These blocked
 * operations will be interrupted throwing a disconnected exception. 
 */
public abstract class Channel {
  Broker broker;

  /**
   * @param broker : the parent Broker
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
   * Not thread-safe. Read bytes in the given array, starting at the given offset.
   * At most "length" bytes will be read. If there are no bytes available, this
   * method will block until there are bytes available or the end of stream is
   * reached.
   * 
   * @param bytes : the array to write on
   * @param offset : the starting index in the array
   * @param length : number of bytes to read
   * @returns the number of bytes read, may not be zero.
   * @throws DisconnectedException if this Channel is disconnected.
   */
  public abstract int read(byte[] bytes, int offset, int length) throws DisconnectedException;

  /**
   * Not thread-safe. Write bytes from the given array, starting at the given
   * offset. At most "length" bytes will be written. If there is no room to write
   * any byte, this method will block until there is room or the end of stream is
   * reached.
   * 
   * @param bytes : the array to read from
   * @param offset : the starting index in the array
   * @param length : number of bytes to write
   * @returns the number of bytes written, may not be zero.
   * @throws DisconnectedException if this Channel is disconnected.
   */
  public abstract int write(byte[] bytes, int offset, int length) throws DisconnectedException;

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
