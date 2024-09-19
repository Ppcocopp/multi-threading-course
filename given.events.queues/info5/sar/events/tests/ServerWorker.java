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

import java.util.LinkedList;
import java.util.List;

import info5.sar.events.queues.events.MessageQueue;
import info5.sar.utils.Executor;
import info5.sar.utils.Panic;

/**
 * This is the server-side of a client session. One instance of this class
 * ServerWorker handles one client, through one queue.
 * 
 * Because a queue may refuse a request to send a message, this worker relies on
 * a background sender for the messages to echo back to clients.
 */
public class ServerWorker implements MessageQueue.Listener {
  private MessageQueue m_queue;
  private String m_name; // broker's name, for debug purposes
  private List<byte[]> m_msgs; // list of messages to echo back

  /*
   * the object in charge of echoing messages back.
   */
  private BackgroundEchoer m_echoer;

  private Executor m_pump;

  ServerWorker(MessageQueue queue) {
    m_name = queue.broker().getName();
    m_queue = queue;
    m_queue.setListener(this);
    m_msgs = new LinkedList<byte[]>();
    m_pump = queue.broker().getEventPump();
    m_echoer = new BackgroundEchoer();
  }

  /**
   * This is the queue callback to notify of a newly received message. The message
   * is to be echoed back, but this cannot be done in this reaction since a queue
   * may refuse a request to send a message. Hence, we used a list of messages to
   * echo and a different object to do it.
   */
  @Override
  public void received(byte[] msg) {
    Executor.check();
    m_msgs.add(msg);
    if (m_msgs.size() == 1)
      m_pump.post(m_echoer);
  }

  @Override
  public void closed() {
    Executor.check();
  }

  /**
   * This is the background sender of messages that must be echoed back to their
   * sender. Nota Bene: since queues do not have a callback to indicate that
   * sending is allowed again after refusing a request, we are forced to have an
   * active polling through repetitive events... without a delay since the
   * executor does not have the capability to post delayed events.
   */
  class BackgroundEchoer implements Runnable {
    // current message this echoer is trying to send.
    private byte[] m_msg;
    // the first and last integer values of the last message that we echoed.
    // this is used to check that messages are received in the proper order
    // and with the proper contents.
    private int m_lastFirst = -1, m_lastLast;

    /**
     * This is where the work is done, trying to echo back one message per event
     * reaction.
     */
    @Override
    public void run() {
      Executor.check();
      if (m_msg != null) {
        if (m_queue.send(m_msg))
          m_msg = null;
        else {
          m_pump.post(this);
          return;
        }
      }
      if (m_msgs.size() == 0)
        return;
      m_msg = m_msgs.remove(0);
      check();
      m_pump.post(this);
    }

    /**
     * this is used to check that messages are received in the proper order and with
     * the proper contents.
     */
    private void check() {
      int first = Test.readInt(m_msg, 0);
      int last = Test.readInt(m_msg, m_msg.length - 4) + 1;
      if (m_lastFirst != -1) {
        if (m_lastLast + 1 != first)
          Panic.failStop("TestServer:_Worker: PANIC");
      }
    }
  }

}
