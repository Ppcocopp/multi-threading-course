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
package info5.sar.channels.tests;

import info5.sar.channels.Broker;
import info5.sar.channels.Channel;
import info5.sar.channels.DisconnectedException;
import info5.sar.channels.Task;

public class TestClient implements Runnable {

  static boolean VERBOSE = false;
  static void log(String s) {
    if (VERBOSE)
      System.out.println(s);
  }
  
  class _Reader implements Runnable {
    Channel ch;
    int nbytes;

    _Reader(Channel ch, int nbytes) {
      this.ch = ch;
      this.nbytes = nbytes;
    }

    @Override
    public void run() {
      try {
        read();
      } catch (DisconnectedException ex) {
        log(name + ":Reader: disconnected.");
      } finally {
        log(name + ":Reader: done.");
      }
    }

    private void read() throws DisconnectedException {
      byte bytes[] = new byte[nbytes];
      int offset = 0;
      int remaining = bytes.length;
      while (remaining != 0) {
        int n = ch.read(bytes, offset, remaining);
        log(name + ":Reader: read " + n + " bytes");
        if (n <= 0)
          throw new Error();
        offset += n;
        remaining -= n;
        log(name + ":Reader: waiting for " + remaining + " bytes");
      }
      log(name + ":Reader: checking " + offset + " bytes");
      for (int i = 0; i < bytes.length; i++)
        if (bytes[i] != (byte) i)
          throw new Error();
      log(name + ":Reader: done");
    }
  }

  class _Writer implements Runnable {
    Channel ch;
    int nbytes;

    _Writer(Channel ch, int nbytes) {
      this.ch = ch;
      this.nbytes = nbytes;
    }

    @Override
    public void run() {
      try {
        write();
      } catch (DisconnectedException ex) {
        log(name + ":Writer: disconnected.");
      } finally {
        log(name + ":Writer: done.");
      }
    }

    private void write() throws DisconnectedException {
      byte bytes[] = new byte[nbytes];
      for (int i = 0; i < bytes.length; i++)
        bytes[i] = (byte) i;

      int offset = 0;
      int remaining = bytes.length;
      while (remaining != 0) {
        int n = ch.write(bytes, offset, remaining);
        log(name + ":Writer: wrote " + n + " bytes");
        if (n <= 0)
          throw new Error();
        offset += n;
        remaining -= n;
      }
    }
  }

  Task reader, writer;
  String connectName;
  int connectPort;
  int nconnects;
  int nbytes;
  int clientNo;
  String name;
  Broker broker;

  TestClient(int no, String name, int port, int nconnects, int nbytes) {
    this.clientNo = no;
    this.connectName = name;
    this.connectPort = port;
    this.nconnects = nconnects;
    this.nbytes = nbytes;
  }

  @Override
  public void run() {
    Task task = (Task) Thread.currentThread();
    name = "Client[" + clientNo + "]";
    System.out.println(name + ": started!");
    broker = task.getBroker();
    for (int i = 0; i < nconnects; i++)
      session();
    System.out.println(name + ": done.");
  }

  private void session() {
    Channel ch = null;
    while (ch == null) {
      ch = broker.connect(connectName, connectPort);
      if (ch == null)
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          // nothing to do...
        }
    }
    reader = new Task(name + ":Reader", broker);
    writer = new Task(name + ":Writer", broker);
    writer.start(new _Writer(ch, nbytes));
    reader.start(new _Reader(ch, nbytes));
    while (reader.alive() || writer.alive()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // nothing to do...
      }
    }
    ch.disconnect();
  }
}
