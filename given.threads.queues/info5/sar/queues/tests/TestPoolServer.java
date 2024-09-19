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
package info5.sar.queues.tests;

import info5.sar.channels.Task;
import info5.sar.queues.ClosedException;
import info5.sar.queues.MessageQueue;
import info5.sar.queues.QueueBroker;
import info5.sar.utils.Panic;

public class TestPoolServer implements Runnable {
  static boolean VERBOSE = false;

  static void log(String s) {
    if (VERBOSE)
      System.out.println(s);
  }

  static void log(Throwable th) {
    if (VERBOSE)
      th.printStackTrace();
  }

  QueueBroker broker;
  int port;
  _Worker[] workers;
  static final int POOL_SIZE = 16;

  TestPoolServer(QueueBroker qb,int port) {
    this.port = port;
    this.broker = qb;
  }

  private void createPool() {
    Task task = (Task) Thread.currentThread();
    workers = new _Worker[POOL_SIZE];
    for (int no = 0; no < workers.length; no++) {
      String name = broker.getName() + ":Worker[" + no + "]";
      workers[no] = new _Worker(name);
    }
  }

  /**
   * Grabs a worker for the newly accepted session.
   * If there are no available worker, this will block
   * until there is a worker available.
   * @return
   */
  private _Worker grabWorker() {
    while (true) {
      for (int no = 0; no < workers.length; no++) {
        _Worker w = workers[no];
        if (w.available())
          return w;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        // nothing to do here.
      }
    }
  }

  /**
   * The entry point for this server,
   * using a pool of tasks to process
   * client sessions.
   */
  @Override
  public void run() {
    int cno = 0;
    MessageQueue queue;
    System.out.println("Server started!");
    try {
      createPool();
      while (true) {
        _Worker w = grabWorker();
        queue = broker.accept(port);
        w.newSession(queue);
      }
    } finally {
      System.out.println("Server done.");
    }
  }

  /**
   * This is a worker for handling one 
   * session from one client.
   */
  class _Worker implements Runnable {
    String name;
    Task task;
    MessageQueue queue;

    _Worker(String name) {
      task = new Task(name, broker.getBroker());
      task.start(this);
    }

    boolean available() {
      return queue == null;
    }

    synchronized void newSession(MessageQueue queue) {
      this.queue = queue;
      notify();
    }

    private void sleep() {
      while (queue == null) {
        try {
          wait();
        } catch (InterruptedException ex) {
          // nothing to do here.
        }
      }
    }

    @Override
    public void run() {
      log(name + ": started.");
      try {
        while (true) {
          synchronized (this) {
            sleep();
            handleSession(queue);
            queue = null;
          }
        }
      } catch (Throwable th) {
        // what should we do here?
        // There is no rationale for an unknown exception,
        // so let's be fail-stop.
        Panic.failStop(th);
      }
    }

    /**
     * Handles one session, echoing back any received message.
     * @param queue
     */
    private void handleSession(MessageQueue queue) {
      log(name + ": new session.");
      try {
        byte bytes[] = new byte[128];
        this.queue = queue;
        while (true) {
          bytes = queue.receive();
          queue.send(bytes, 0, bytes.length);
          log(name + ": echoed " + bytes.length + " bytes!");
        }
      } catch (ClosedException ex) {
        log(name + ": session done.");
      }
    }

  }

}
