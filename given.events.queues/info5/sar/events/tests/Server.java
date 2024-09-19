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
package info5.sar.events.tests;

import info5.sar.events.queues.events.MessageQueue;
import info5.sar.events.queues.events.QueueBroker;
import info5.sar.events.queues.events.QueueBroker.AcceptListener;

/**
 * This is a simple echo server.
 * It accepts any connection from clients.
 * Per client, it echoes back any message that it receives,
 * see the class ServerSession.
 */
public class Server implements Runnable {
  static boolean VERBOSE = true;

  private static void log(String s) {
    if (VERBOSE)
      System.out.print(s);
  }

  private static void logln(String s) {
    if (VERBOSE)
      System.out.println(s);
  }

  private static void log(Throwable th) {
    if (VERBOSE)
      th.printStackTrace();
  }

  private QueueBroker broker;
  private int port;  // port to bind in order to accept connections on.

  Server(QueueBroker qb, int port) {
    this.port = port;
    this.broker = qb;
  }

  private class _AcceptListener implements AcceptListener {
    @Override
    public void accepted(MessageQueue queue) {
      new ServerWorker(queue);
    }
  }

  /**
   * This is the reaction to the initial event,
   * starting the server and binding the given port.
   */
  @Override
  public void run() {
    System.out.println("Server started!");
    broker.bind(port, new _AcceptListener());
  }

}
