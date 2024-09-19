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

import info5.sar.utils.Panic;

/*
 * Test-level message. 
 * Assumes a special encoding through channels 
 * where each message payload is a chunk of 
 * a global sequence of increasing integers,
 * per channel of course. 
 * Therefore, each payload is a partial sequence 
 * of 4-byte-encoded integers.
 * This allows to check that messages are received
 * intact and in sequence.
 */

public class Message {
  int seqno;       // the numero of this message in the global sequence
  byte[] payload;  // sequence of increasing 4-byte-encoded integers
  int first, last; // first and last encoded integers in the payload
  int size;        // number of 4-byte-encoded integers.
  Message(int seqno, byte[] payload) {
    this.size = payload.length >> 2;
    this.seqno = seqno;
    this.payload = payload;
    first = Test.readInt(payload, 0);
    last = Test.readInt(payload, payload.length - 4);
  }

  void echoRange(PrintStream ps) {
    ps.println("Msg[" + seqno + "]: [" + first + ":" + last + "]");
  }

  void check(int lastSeqno, int lastFirst, int lastLast) {
    if (lastFirst != -1) {
      if (lastLast + 1 != first) {
        String s = "ClientSession:Message: broken sequence\n";
        s += " last["+lastSeqno+"]:" + lastFirst + ":" + lastLast;
        s += " msg["+seqno+"]:" + first + ":" + last;
        Panic.failStop(s);
      }          
    }
  }

  /*
   * Used to check if this message is the correct one
   * in the global sequence of transmitted integers
   */
  int check(String name, int pos) {
    for (int i = 0; i < size; i++) {
      int value = Test.readInt(payload, 4 * i);
      if (value != pos + i) {
        System.err.print(name + " expecting [" + pos + ":" + (pos + size) + "[ ");
        System.err.println("  received: [" + first + ":" + last + "[");
        System.err.println("PANIC: corrupted message");
        // throw new Error("PANIC: corrupted message");
        Test.failStop();
      }
    }
    return size;
  }

}
