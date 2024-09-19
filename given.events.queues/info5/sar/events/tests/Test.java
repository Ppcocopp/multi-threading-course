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

import java.lang.reflect.Constructor;

import info5.sar.events.channels.Broker;
import info5.sar.events.queues.events.QueueBroker;
import info5.sar.utils.Executor;
import info5.sar.utils.Panic;

/**
 * This test is a simple echo test based on a client-server architecture. The
 * server accepts connections from clients and echoes whatever message it
 * receives back to the client that sent it.
 * 
 * Without special arguments, this test uses the following classes for the queue
 * and channel layers:
 * 
 * - info5.sar.channels.CBroker - info5.sar.events.queues.CQueueBroker
 * 
 * But you may change these classes with your own with the arguments
 * -cbroker:my.pkg.MyBroker -qbroker:my.pkg.MyQueueBroker if your channel broker
 * is implemented by the class my.pkg.MyBroker and your queue broker is
 * implement by the class my.pkg.MyQueueBroker.
 * 
 * 
 * 
 * You can control the test via arguments given when launching:
 * 
 * -nclients: the number of clients with the argument -nconnects: the number of
 * times a client sequentially connects with the server, each connection being a
 * session. -nbytes: the total number of bytes sent by a client within a
 * session. -msize: the size of the message used to chunk up the total number of
 * bytes to send.
 * 
 * At first, start simple with one client, connecting only once, and sending
 * only one message:
 * 
 * -nclients:1 -nconnects:1 -nbytes:100 -msize:100
 * 
 */
public class Test {

  static void ensure(boolean cond) {
    Panic.ensure(cond);
  }

  static void failStop() {
    Panic.failStop();
  }

  static void failStop(Throwable th) {
    Panic.failStop(th);
  }

  static String ChannelBrokerClassName = "info5.sar.events.queues.CBroker";
  static String QueueBrokerClassName = "info5.sar.events.queues.CQueueBroker";
  static final String CBROKER_OPTION = "-cbroker:";
  static final String QBROKER_OPTION = "-qbroker:";
  static final String NCLIENTS_OPTION = "-nclients:";
  static final String NCONNECTS_OPTION = "-nconnects:";
  static final String NBYTES_OPTION = "-nbytes:";
  static final String MSIZE_OPTION = "-msize:";

  static void parseArgs(String args[]) {
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith(CBROKER_OPTION))
        ChannelBrokerClassName = arg.substring(CBROKER_OPTION.length());
      if (arg.startsWith(QBROKER_OPTION))
        QueueBrokerClassName = arg.substring(QBROKER_OPTION.length());
      if (arg.startsWith(NCLIENTS_OPTION))
        nclients = Integer.valueOf(arg.substring(NCLIENTS_OPTION.length()));
      if (arg.startsWith(NCONNECTS_OPTION))
        nconnects = Integer.valueOf(arg.substring(NCONNECTS_OPTION.length()));
      if (arg.startsWith(NBYTES_OPTION))
        nbytes = Integer.valueOf(arg.substring(NBYTES_OPTION.length()));
      if (arg.startsWith(MSIZE_OPTION)) {
        msize = Integer.valueOf(arg.substring(MSIZE_OPTION.length()));
        msize = 4*(msize/4); // ensure a multiple of four.
      }
    }
  }

  static Class cbroker_cls;
  static Constructor cbroker_ctor;
  static Class qbroker_cls;
  static Constructor qbroker_ctor;

  /**
   * This loads the classes for the channel broker and
   * queue broker, grabbing the required constructors
   * with the following arguments:
   *   (String name) for the channel broker
   *   (Executor pump, Broker) for the queue broker.
   * By default, it looks for given implementation
   * classes:
   *   info5.sar.channels.CBroker
   *   info5.sar.events.queues.CQueueBroker
   */
  static private void loadBrokerClasses() throws Exception {
    if (cbroker_cls == null) {
      cbroker_cls = Class.forName(ChannelBrokerClassName);
      Class params[] = new Class[2];
      params[0] = String.class;
      params[1] = Executor.class;
      cbroker_ctor = cbroker_cls.getConstructor(params);
    }
    if (qbroker_cls == null) {
      qbroker_cls = Class.forName(QueueBrokerClassName);
      Class params[] = new Class[2];
      params[0] = Executor.class;
      params[1] = Broker.class;
      qbroker_ctor = qbroker_cls.getConstructor(params);
    }
  }

  /**
   * Prints at startup the options used for this test run
   */
  static void printOptions() {
    System.out.println("--------------------------------------");
    System.out.println("Using Channel Broker: " + ChannelBrokerClassName);
    System.out.println("Using Queue Broker: " + QueueBrokerClassName);
    System.out.println("--------------------------------------");
    System.out.println("  nclients=" + nclients);
    System.out.println("  nconnects=" + nconnects);
    System.out.println("  nbytes=" + nbytes);
    System.out.println("  msize=" + msize);
    System.out.println("--------------------------------------\n\n");
  }

  /*
   * Default values for the options.
   */
  private static int nclients = 1;
  private static int nconnects = 1;
  private static int nbytes = 100;
  private static int msize = 100;
  private static String name = "Server";
  private static int port = 80;

  
  public static void main(String args[]) throws Exception {
    parseArgs(args);
    printOptions();
    loadBrokerClasses();
    Test test = new Test();
    test.run();
    System.out.println("That's all folks.");
    System.exit(0);
  }

  /*
   * These non-static fields are for debug purposes,
   * allowing to access these objects easily
   * when debugging since this object Test
   * is always on the stack.
   */
  private Executor m_pump;        
  private Server m_server;    
  private Client[] m_clients; 

  /**
   * simple constructor, does not start 
   * the event pump.
   */
  private Test() throws Exception {
    String name = "Server";
    m_pump = new Executor("Event Pump");
    createServer(name, port);
    createClients(name, port);
  }

  /**
   * starts the pump on its own thread
   * and then wait for all clients to finish.
   */
  private void run() {
    m_pump.start();
    waitForClients();
  }

  private void waitForClients() {
    while (true) {
      int nclients = 0;
      for (int i = 0; i < m_clients.length; i++) {
        Client tc = m_clients[i];
        if (!tc.done()) {
          nclients++;
          if (false) // set to true for debugging purposes
            m_pump.post(new Runnable() {
              @Override
              public void run() {
                tc.dump(System.out);
              }
            });
        }
      }
      if (nclients == 0)
        break;
      else
        System.out.println("--- waiting on " + nclients + " clients");
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        // nothing to do...
      }
    }
  }

  private void createServer(String name, int port) throws Exception {
    QueueBroker qb = newBrokers(name);
    m_server = new Server(qb, port);
    m_pump.post(m_server);
  }

  private void createClients(String sname, int sport) throws Exception {
    m_clients = new Client[nclients];
    for (int i = 0; i < nclients; i++) {
      String cn = "Client" + i;
      QueueBroker qb = newBrokers(cn);
      m_clients[i] = new Client(qb, i, sname, sport, nconnects, nbytes, msize);
      m_pump.post(m_clients[i]);
    }
  }

  /**
   * Creates a pair of channel broker and queue broker,
   * both with the same name.
   */
  private QueueBroker newBrokers(String name) throws Exception {
    Object[] args = new Object[] { name, m_pump };
    Broker cb = (Broker) cbroker_ctor.newInstance(args);
    args = new Object[] { m_pump, cb };
    QueueBroker qb = (QueueBroker) qbroker_ctor.newInstance(args);
    return qb;
  }

  public static void writeInt(byte[] msg, int offset, int value) {
    msg[offset] = (byte) (value >>> 24);
    msg[offset + 1] = (byte) (value >>> 16 & 0xFF);
    msg[offset + 2] = (byte) (value >>> 8 & 0xFF);
    msg[offset + 3] = (byte) (value & 0xFF);
  }

  public static int readInt(byte[] msg, int offset) {
    int value;
    value = (msg[offset + 0] & 0xFF) << 24;
    value |= (msg[offset + 1] & 0xFF) << 16;
    value |= (msg[offset + 2] & 0xFF) << 8;
    value |= (msg[offset + 3] & 0xFF);
    return value;
  }
}
