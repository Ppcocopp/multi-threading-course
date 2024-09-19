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

import java.lang.reflect.Constructor;

import info5.sar.channels.Broker;
import info5.sar.channels.Task;
import info5.sar.queues.QueueBroker;
import info5.sar.utils.Panic;

/**
 * This test is a simple echo test based on a client-server architecture. The
 * server accepts connections from clients and echoes whatever message it
 * receives back to the client that sent it.
 * 
 * Two different servers are provided, one not using a pool of tasks to
 * process client sessions and one with a pool of tasks.
 * 
 * Without special arguments, this test uses the following classes for the queue
 * and channel layers:
 * 
 * - info5.sar.channels.CBroker 
 * - info5.sar.events.queues.CQueueBroker
 * 
 * But you may change these classes with your own with the arguments
 *   -cbroker:my.pkg.MyBroker 
 *   -qbroker:my.pkg.MyQueueBroker 
 *   
 * if your channel broker is implemented by the class my.pkg.MyBroker 
 * and your queue broker is implement by the class my.pkg.MyQueueBroker.
 * 
 * You can control the test via arguments given when launching:
 * 
 *   -nclients: the number of clients with the argument 
 *   -nconnects: the number of times a client sequentially connects 
 *               with the server, each connection being a session. 
 *   -nbytes: the total number of bytes sent by a client within a session. 
 *   -pool: controls which server is instanciated, the one with a pool of
 *          tasks or not. The default is without using a pool.
 * 
 * At first, start simple with one client, connecting only once, and sending
 * only one message:
 * 
 * -nclients:1 -nconnects:1 -nbytes:100
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

  private static String ChannelBrokerClassName = "info5.sar.channels.CBroker";
  private static String QueueBrokerClassName = "info5.sar.queues.CQueueBroker";
  private static final String CBROKER_OPTION = "-cbroker:";
  private static final String QBROKER_OPTION = "-qbroker:";
  private static final String NCLIENTS_OPTION = "-nclients:";
  private static final String NCONNECTS_OPTION = "-nconnects:";
  private static final String NBYTES_OPTION = "-nbytes:";
  private static final String POOL_OPTION = "-pool";

  private static void parseArgs(String args[]) {
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
      if (arg.equals(POOL_OPTION))
        usePool = true;
    }
  }

  static void printOptions() {
    System.out.println("--------------------------------------");
    System.out.println("Using Channel Broker: " + ChannelBrokerClassName);
    System.out.println("Using Queue Broker: " + QueueBrokerClassName);
    System.out.println("--------------------------------------");
    System.out.println("  nclients=" + nclients);
    System.out.println("  nconnects=" + nconnects);
    System.out.println("  nbytes=" + nbytes);
    System.out.println("  using server-side thread pool: " + usePool);
    System.out.println("--------------------------------------\n\n");
  }

  private static int nclients = 2;
  private static int nconnects = 2;
  private static int nbytes = 1024;
  private static boolean usePool = false;

  public static void main(String args[]) throws Exception {
    String name = "Server";
    int port = 80;
    Task tcs[];

    parseArgs(args);
    printOptions();

    createServer(name,port);
    tcs = createClients(name,port);
    waitForClients(tcs);
    System.out.println("\n\nThat's all folks...");
    System.exit(0);
  }
  
  /**
   * Creates the server
   * @param name:  broker's name of the server
   * @param port:  port that the server will accept on
   * @throws Exception if anything goes wrong.
   */
  private static void createServer(String name, int port) throws Exception {
    Task ts, tc;
    TestServer s;
    TestClient c;
    QueueBroker qb = newBrokers(name);
    ts = new Task(name, qb.getBroker());
    if (usePool)
      ts.start(new TestPoolServer(qb,port));
    else
      ts.start(new TestServer(port));
      
  }
  
  /**
   * Creates all the clients.
   * @param name:  broker's name of the server
   * @param port:  port that the server will accept on
   * @throws Exception if anything goes wrong.
   */
  private static Task[] createClients(String name, int port) throws Exception {
    Task tcs[] = new Task[nclients];
    TestClient c;
    for (int i = 0; i < nclients; i++) {
      String cn = "Client" + i;
      QueueBroker qb = newBrokers(cn);
      Task tc = new Task(cn, qb.getBroker());
      tcs[i] = tc;
      c = new TestClient(qb,i, name, port, nconnects, nbytes);
      tc.start(c);
    }
    return tcs;
  }

  /**
   * Wait until all clients to terminate.
   */
  private static void waitForClients(Task tcs[]) {
    while (true) {
      int ntasks = 0;
      for (int i = 0; i < nclients; i++) {
        Task tc = tcs[i];
        if (tc != null && !tc.dead())
          ntasks++;
      }
      if (ntasks == 0)
        break;
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // nothing to do...
      }
    }
  }

  private static Class cbroker_cls;
  private static Constructor cbroker_ctor;
  private static Class qbroker_cls;
  private static Constructor qbroker_ctor;

  /**
   * Creates a pair of channel broker and queue broker,
   * both with the same name.
   */
  private static QueueBroker newBrokers(String name) throws Exception {
    if (cbroker_cls == null) {
      cbroker_cls = Class.forName(ChannelBrokerClassName);
      Class params[] = new Class[1];
      params[0] = String.class;
      cbroker_ctor = cbroker_cls.getConstructor(params);
    }
    if (qbroker_cls == null) {
      qbroker_cls = Class.forName(QueueBrokerClassName);
      Class params[] = new Class[1];
      params[0] = Broker.class;
      qbroker_ctor = qbroker_cls.getConstructor(params);
    }
    Broker cb = (Broker)cbroker_ctor.newInstance(new Object[] { name });
    QueueBroker qb = (QueueBroker)qbroker_ctor.newInstance(new Object[] { cb});
    return qb;
  }

}
