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

import java.io.PrintStream;

import info5.sar.events.queues.events.QueueBroker;
import info5.sar.utils.Executor;
import info5.sar.utils.Logger;

/**
 * Each client will go through one or more sessions, each session being an 
 * exchange of messages through a single queue. 
 */
public class Client implements Runnable {

  static boolean SILENT = false;
  synchronized void log(String s) {
    m_logger.log(s);
  }
  synchronized void logln(String s) {
    m_logger.logln(s);
  }

  String m_connectName;
  int m_connectPort;

  // the number of sessions that this client will go through
  private int m_nsessions;
  // the current session, sessions being done sequentially.
  private ClientSession m_cs;
  
  // the number of 4-byte integer values to send per session.
  private int m_nvalues;
  // the size of each message in bytes, a multiple of four.
  private int m_msize;
  // remaining number of 4-byte integer values to send.
  private int m_remaining;
  
  // this client's name, numero, and broker
  private int m_clientNo;
  private String m_name;
  private QueueBroker m_broker;
  // used to generate unique client session identifiers.
  private int m_csid;
  Logger m_logger;

  Client(QueueBroker broker, int no, String name, int port, int nsessions, int nvalues, int msize) {
    m_broker = broker;
    m_clientNo = no;
    m_connectName = name;
    m_connectPort = port;
    m_nsessions = nsessions;
    m_nvalues = nvalues;
    m_msize = msize;
    m_remaining = nsessions * nvalues;
    m_name = "Client[" + m_clientNo + "]";
    m_logger = new Logger(m_name,SILENT,System.out);
  }

  /**
   * For debug purposes only
   */
  void dump(PrintStream ps) {
    System.out.println();
    System.out.println(m_name);
    System.out.println("  nconnects="+m_nsessions);
    System.out.println("  csid="+m_csid);
    System.out.println("  remaining="+m_remaining);
    if (m_cs!=null) 
      m_cs.dump(ps);
  }

  QueueBroker broker() {
    return m_broker;
  }

  String name() {
    return m_name;
  }

  /**
   * Creates the first session, the other session will
   * be created in the callback "done(ClientSession)", 
   * see below.
   */
  @Override
  public void run() {
    Executor pump = m_broker.getEventPump();
    System.out.println(m_name + ": started!");
    m_cs = new ClientSession(this, m_csid++, m_nvalues, m_connectName, m_connectPort,m_msize);
    pump.post(m_cs);
  }

  /**
   * This is the callback from the current session
   * to indicate that it is done and the next session
   * should be started, if this client is not done sending
   * all the 4-byte-encoded integer values that it was
   * supposed to send.
   */
  void done(ClientSession cs) {
    Executor pump = m_broker.getEventPump();
    m_remaining -= m_nvalues;
    if (m_csid < m_nsessions) {
      m_cs = new ClientSession(this, m_csid++, m_nvalues, m_connectName, m_connectPort,m_msize);
      pump.post(m_cs);
    } else {
      m_done = true;
      System.out.println("==== " + cs.name() + " done.");
    }
  }

  boolean m_done;

  /**
   * Tells if this client is done with its sessions or not.
   */
  public boolean done() {
    return m_done;
  }
}
