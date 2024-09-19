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

/**
 * A simple client that connects one or more time to the server.
 * Each connection is called a session, during which this client
 * sends to the server a certain number of bytes, split in 
 * small messages and awaits that the server echoes them all back.
 */
public class TestClient implements Runnable {

  // the size of one small message:
  static final int MESSAGE_SIZE=16;
  
  static boolean VERBOSE = true;
  static void log(String s) {
    if (VERBOSE)
      System.out.println(s);
  }
  
  /**
   * This is the reader that will read all the messages
   * echoed back from the server, on its own task. 
   * For each received message, it checks that the 
   * contents is not corrupted.
   */
  class _Reader implements Runnable {
    MessageQueue queue;
    int nbytes;
    int offset;
    
    _Reader(MessageQueue queue, int nbytes) {
      this.queue = queue;
      this.nbytes = nbytes;
    }

    /**
     * Loops over reading messages until all
     * messages have been read.
     */
    @Override
    public void run() {
      try {
        while (offset<nbytes) 
          receive();
      } catch (ClosedException ex) {
        log(name + ":Reader: disconnected.");
      } finally {
        log(name + ":Reader: done.");
      }
    }

    /**
     * Reads one message and checks that its content
     * has not been corrupted. This is possible because
     * each message is a small chunk of a larger sequence
     * of increasing integer values casted down to bytes.
     * @throws ClosedException
     */
    private void receive() throws ClosedException {
      byte bytes[];
      bytes = queue.receive();
      log(name + ":Reader: read " + bytes.length + " bytes");
      for (int i = 0; i < bytes.length; i++)
        if (bytes[i] != (byte) (offset+i))
          throw new Error();
      log(name + ":Reader: done");
      offset+=bytes.length;
    }
  }

  /**
   * This is the writer that will send a certain number
   * of messages to the server, on its own task. 
   * Each message is a small chunk of a larger sequence
   * of increasing integer values casted down to bytes.
   */
  class _Writer implements Runnable {
    MessageQueue ch;
    int nbytes;
    int offset;

    _Writer(MessageQueue ch, int nbytes) {
      this.ch = ch;
      this.nbytes = nbytes;
    }

    /**
     * Loops over sending messages until all
     * messages have been read.
     */
    @Override
    public void run() {
      try {
        while (offset<nbytes) {
          send();
        }
      } catch (ClosedException ex) {
        log(name + ":Writer: disconnected.");
      } finally {
        log(name + ":Writer: done.");
      }
    }

    /**
     * Sends one message, as a small chunk of the larger sequence
     * of increasing integer values casted down to bytes.
     * @throws ClosedException
     */
    private void send() throws ClosedException {
      byte bytes[];
      int size,remaining = nbytes-offset;
      if (remaining>=32)
        size = 32;
      else
        size = remaining;
      bytes = new byte[size];
      for (int i = 0; i < size; i++)
        bytes[i] = (byte) (offset+i);
      ch.send(bytes,0,bytes.length);
      offset+=size;
      log(name + ":Writer: wrote " + size + " bytes");
    }
  }
  
  
  Task reader, writer;
  String connectName;
  int connectPort;
  int nconnects;
  int nbytes;
  int clientNo;
  String name;
  QueueBroker broker;

  TestClient(QueueBroker broker, int no, String name, int port, int nconnects, int nbytes) {
    this.broker = broker;
    this.clientNo = no;
    this.connectName = name;
    this.connectPort = port;
    this.nconnects = nconnects;
    this.nbytes = nbytes;
  }

  /**
   * This client will go through one or more sessions,
   * each session corresponding to one connection to the server
   * and sending "nbytes" in small messages.
   */
  @Override
  public void run() {
    Task task = (Task) Thread.currentThread();
    name = "Client[" + clientNo + "]";
    System.out.println(name + ": started!");
    for (int i = 0; i < nconnects; i++)
      session();
    System.out.println(name + ": done.");
  }

  /**
   * Each connection is called a session, during which this client
   * sends to the server a certain number of bytes, split over small
   * messages and awaits that the server echoes them all back.
   * 
   * NOTA BENE: notice that a session closes the message queue, only
   * once it has received all the messages echoed back by the server,
   * hence closing the queue only once both the writer and reader tasks
   * are dead. This must be so or otherwise the server will not be able 
   * to echo some of the last sent messages. Indeed, it will be allowed to 
   * receive them from a close queue but not allowed to echo them back 
   * through a closed queue.
   */
  private void session() {
    MessageQueue queue = null;
    while (queue == null) {
      queue = broker.connect(connectName, connectPort);
      if (queue == null)
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          // nothing to do...
        }
    }
    reader = new Task(name + ":Reader", broker.getBroker());
    writer = new Task(name + ":Writer", broker.getBroker());
    writer.start(new _Writer(queue, nbytes));
    reader.start(new _Reader(queue, nbytes));
    while (reader.alive() || writer.alive()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // nothing to do...
      }
    }
    queue.close();
  }
}
