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

import java.lang.reflect.Constructor;

import info5.sar.channels.Broker;
import info5.sar.channels.Task;

public class Test {

  static void ensure(boolean cond) {
    if (!cond)
      failStop();
  }
  static void failStop() {
    System.exit(-1);
  }
  static void failStop(Throwable th) {
    th.printStackTrace();
    failStop();
  }

  static String ClassName = "info5.sar.channels.CBroker";
  static final String BROKER_OPTION = "-broker:";
  static final String NCLIENTS_OPTION = "-nclients:";
  static final String NCONNECTS_OPTION = "-nconnects:";
  static final String NBYTES_OPTION = "-nbytes:";

  static void parseArgs(String args[]) {
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith(BROKER_OPTION))
        ClassName = arg.substring(BROKER_OPTION.length());
      if (arg.startsWith(NCLIENTS_OPTION))
        nclients = Integer.valueOf(arg.substring(NCLIENTS_OPTION.length()));
      if (arg.startsWith(NCONNECTS_OPTION))
        nconnects = Integer.valueOf(arg.substring(NCONNECTS_OPTION.length()));
      if (arg.startsWith(NBYTES_OPTION))
        nbytes = Integer.valueOf(arg.substring(NBYTES_OPTION.length()));
    }
  }

  static void printOptions() {
    System.out.println("--------------------------------------");
    System.out.println("Using Broker: " + ClassName);
    System.out.println("--------------------------------------");
    System.out.println("  nclients=" + nclients);
    System.out.println("  nconnects=" + nconnects);
    System.out.println("  nbytes=" + nbytes);
    System.out.println("--------------------------------------\n\n");
  }

  static int nclients = 2;
  static int nconnects = 2;
  static int nbytes = 1024;

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
  
  private static void createServer(String name, int port) throws Exception {
    Task ts, tc;
    TestServer s;
    TestClient c;
    ts = new Task(name, newBroker(name));
    // ts.start(new TestServer(port));
    ts.start(new TestPoolServer(port));
  }
  
  private static Task[] createClients(String name, int port) throws Exception {
    Task tcs[] = new Task[nclients];
    TestClient c;
    for (int i = 0; i < nclients; i++) {
      String cn = "Client" + i;
      Task tc = new Task(cn, newBroker(cn));
      tcs[i] = tc;
      c = new TestClient(i, name, port, nconnects, nbytes);
      tc.start(c);
    }
    return tcs;
  }

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

  static Class cls;
  static Constructor ctor;

  static Broker newBroker(String name) throws Exception {
    if (cls == null) {
      cls = Class.forName(ClassName);
      Class params[] = new Class[1];
      params[0] = String.class;
      ctor = cls.getConstructor(params);
    }
    return (Broker) ctor.newInstance(new Object[] { name });
  }

}
