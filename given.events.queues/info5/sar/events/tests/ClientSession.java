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

import info5.sar.events.queues.events.MessageQueue;
import info5.sar.events.queues.events.QueueBroker;
import info5.sar.utils.Executor;
import info5.sar.utils.Logger;
import info5.sar.utils.Panic;

/**
 * This represents a client session, with the server, that is, the communication
 * between the connect and the disconnect. Each client will go through one or
 * more sessions. Each session sends messages that are echoed back by the
 * server. Each message is a chunk of a sequence of integers, each integer being
 * encoded over 4 bytes, with the usual network order of bytes.
 */
public class ClientSession implements Runnable, QueueBroker.ConnectListener {
  // this session's name and number
  private String m_name;
  private int m_no;
  // the client owning this session
  private Client m_tc;
  // this session parameters:
  private int m_length; // the number of 4-byte-encoded integer values to send
  private int m_offset; // the current offset in the sequence of integer values to send
  private int m_msize; // the message size to use in bytes (multiple of 4).

  // connect parameters: name and port.
  private String m_cname;
  private int m_cport;

  // the objects in charge of sending and receiving messages
  private _Reader m_reader;
  private _Writer m_writer;

  private MessageQueue m_queue;
  private Logger m_logger;

  ClientSession(Client tc, int no, int length, String cname, int cport, int msize) {
    m_tc = tc;
    m_no = no;
    m_msize = msize;
    m_length = length;
    m_offset = 0;
    m_cname = cname;
    m_cport = cport;
    m_name = tc.name() + "[" + m_no + "]";
    m_logger = new Logger(m_name, Client.SILENT, System.out);
  }

  String name() {
    return m_name;
  }

  /**
   * Used for debug purposes only.
   */
  void dump(PrintStream ps) {
    ps.println(" " + m_name);
    if (m_queue != null) {
      ps.println("  queue.closed=" + m_queue.closed());
      m_writer.dump(ps);
      m_reader.dump(ps);
    } else
      ps.println(" never connected");
  }

  /**
   * This client session is done, it will notify the client.
   */
  private void done() {
    System.out.println(m_name + " done.");
    m_tc.done(this);
  }

  /**
   * This is the starting point of the execution of this session, attempting to
   * connect to the server. *** REJECTED REQUESTS ARE NOT SUPPORTED ***
   */
  @Override
  public void run() {
    QueueBroker broker = m_tc.broker();
    if (!broker.connect(m_cname, m_cport, this))
      Panic.failStop("Session[" + m_no + "] connect request refused.");
  }

  /**
   * Queue Broker Listener callback indicating that a connection has been
   * established. This creates the reader and writer that will send and receive
   * messages.
   */
  public void connected(MessageQueue queue) {
    QueueBroker broker = m_tc.broker();
    Executor pump = broker.getEventPump();

    m_queue = queue;
    m_reader = new _Reader(m_offset, m_length);
    queue.setListener(m_reader);
    m_writer = new _Writer(pump, m_offset, m_length, m_msize);

    // this will start the sending of messages.
    pump.post(m_writer);
  }

  /**
   * Queue Broker Listener callback indicating that a connection has been refused.
   * *** NOT SUPPORTED ***
   */
  public void refused() {
    Panic.failStop("Session[" + m_no + "] refused.");
  }

  /**
   * This is the reader of messages echoed by the server. We do nothing with these
   * messages except checking that they are received intact and in the proper
   * sequence. See the method Message:check(String,int).
   * 
   * NOTA BENE: it is the reader that closes the message queue, once it has
   * received all the messages echoed back by the server. This must be so or
   * otherwise the server will not be able to echo some of the last messages.
   * Indeed, it will be allowed to receive them from a close queue but not allowed
   * to echo them back through a closed queue.
   */
  private class _Reader implements MessageQueue.Listener {
    // holds the position in the global sequence of messages
    private int m_pos;
    // holds the remaining number of 4-byte-encoded integer values
    private int m_remaining;
    // used to give messages a sequence number. helps debugging.
    private int m_seqno;
    // holds the first and last 4-byte-encode integer of the message last received
    private int lastFirst = -1, lastLast;

    _Reader(int position, int length) {
      m_pos = position;
      m_remaining = length;
    }

    /**
     * For debug purposes only
     */
    void dump(PrintStream ps) {
      System.out.println("  reader:");
      System.out.println("    pos=" + m_pos);
      System.out.println("    remaining=" + m_remaining);
    }

    @Override
    public void received(byte[] bytes) {
      Executor.check();
      Message msg = new Message(m_seqno++, bytes);
      int size = msg.check(m_name, m_pos);
      m_pos += size;
      m_remaining -= size;
      if (m_remaining == 0) {
        // close the queue only once we got
        // all our messages echoed.
        m_queue.close();
        m_logger.logln(m_name + " received all.");
        done();
      }
    }

    @Override
    public void closed() {
      Executor.check();
      m_logger.logln(m_name + " done");
      done();
    }
  }

  /**
   * This is the writer that sends messages that are echoed back by the server.
   * Each message is a chunk of a sequence of integers, each integer being encoded
   * over 4 bytes, with the usual network order of bytes.
   * 
   * NOTA BENE: this write does not close the message queue, once it has sent all
   * its messages. Doing so would prevent the server to be able to echo some of
   * the last messages. Indeed, the server will be allowed to receive all sent
   * messages, even from a close queue, but not allowed to echo them back through
   * a closed queue.
   */
  private class _Writer implements Runnable {
    private int m_remaining;
    private int m_pos;
    private int m_msize;
    private int m_seqno;
    private Message m_msg;
    private int lastFirst = -1, lastLast, lastSeqno;
    private Executor m_pump;

    _Writer(Executor pump, int offset, int length, int msize) {
      m_pump = pump;
      m_remaining = length;
      m_pos = offset;
      m_msize = msize;
    }

    /**
     * For debug purposes only
     */
    void dump(PrintStream ps) {
      System.out.println("  writer:");
      System.out.println("    msize=" + m_msize);
      System.out.println("    pos=" + m_pos);
      System.out.println("    remaining=" + m_remaining);
    }

    /**
     * Creates a new message as part of a global sequence of 4-byte-encoded integer
     * values.
     * 
     * @return
     */
    Message newMessage() {
      byte bytes[];
      int size;
      if (m_remaining > m_msize)
        size = m_msize;
      else
        size = m_remaining;
      bytes = new byte[4 * size];
      for (int i = 0; i < size; i++) {
        Test.writeInt(bytes, 4 * i, m_pos + i);
      }
      m_logger.logln(m_name + " sending [" + m_pos + ":" + (m_pos + size) + "[");
      m_pos += size;
      m_remaining -= size;
      return new Message(m_seqno++, bytes);
    }

    /**
     * This sends one message per event, as long as the queue does not refuse to
     * send a message, in which case this reaction will attempt over and over to
     * resend the same message until it goes through the queue. Nota Bene: we do not
     * close the queue when done sending messages.
     */
    @Override
    public void run() {
      Executor.check();
      if (m_msg == null)
        m_msg = newMessage();
      m_msg.check(lastSeqno, lastFirst, lastLast);

      if (m_queue.send(m_msg.payload)) {
        lastSeqno = m_msg.seqno;
        lastFirst = m_msg.first;
        lastLast = m_msg.last;
        m_msg = null;
        if (m_remaining > 0) {
          m_pump.post(this);
        } else {
          // Do not close the queue here, the reader
          // will do it when it has received all messages.
          m_logger.logln(m_name + " sent all.");
        }
      } else {
        m_pump.post(this);
      }
    }
  }

}
