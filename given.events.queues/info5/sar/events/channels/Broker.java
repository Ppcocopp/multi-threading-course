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

/**
 * Brokers are there to permit to establish channels.<br>
 * Each broker must be uniquely named. <br>
 * Each broker may be used to accept on different ports concurrently,<br>
 * creating a new channel for each newly accepted connection.
 */
public abstract class Broker {
  String name;
  /**
   * Each Broker must be uniquely named. 
   * @param name : name of the Broker
   * @param executor : the executor event pump
   * @throws IllegalArgumentException if the name is not unique.
   */
  protected Broker(String name) {
    this.name = name;
  }
  
  /**
   * @returns the name of this Broker.
   */
  public String getName() { return name; }
  
  /**
   * Listener for Broker
   * Allows to define the behavior when a connection is accepted and fully-connected.
   */
  public interface AcceptListener {
	  /**
	   * Defines the behavior when a connection is accepted and fully-connected.
	   * @param channel : the resulting Channel
	   */
	  public void accepted(Channel channel);
  }
  
  /**
   * Indicate that this Broker will accept one connection on the given port.<br>
   * This will create a fully connected Channel returned via the listener.<br>
   * This is a thread-safe non-blocking rendez-vous. 
   * @param port : the connection port
   * @param listener : the listener to call when connected
   * @throws IllegalArgumentException if there is already 
   *         an accept pending on the given port.
   */
  public abstract void accept(int port, AcceptListener listener);
  
  /**
   * Listener for Broker
   * Allows to define the behavior when a connection is fully-connected.
   */
  public interface ConnectListener {
	  /**
	   * Defines the behavior when a connection is fully-connected.
	   * @param channel : the resulting Channel
	   */
	  public void connected(Channel channel);
  }
  
  /**
   * Attempts a connection to the given port, via the Broker with the given name. <br>
   * This will create a fully connected Channel returned via the listener.<br>
   * This is a thread-safe non-blocking rendez-vous. <br>
   * Note: multiple accepts from different tasks with 
   *       the same name and port are legal
   * @param name : name of the Broker to connect to.
   * @param port : the connection port
   * @param listener : the listener to call when connected
   * @return True if the distant Broker has been found, false otherwise.
   */
  public abstract boolean connect(String name, int port, ConnectListener listener);
  
}
