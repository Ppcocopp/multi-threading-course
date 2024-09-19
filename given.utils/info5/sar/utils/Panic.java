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
package info5.sar.utils;

public class Panic {
  public static void ensure(boolean cond) {
    if (!cond)
      failStop();
  }

  public static void failStop() {
    failStop("fail stop.", null);
  }

  public static void failStop(String msg) {
    failStop(msg, null);
  }

  public static void failStop(String msg, Throwable th) {
    if (th == null) {
      try {
        throw new Error();
      } catch (Error e) {
        th = e;
      }
    }
    synchronized (System.out) {
      synchronized (System.err) {
        System.err.println("\n\n==============================");
        System.err.println("PANIC: " + msg);
        th.printStackTrace(System.err);
        System.err.flush();
        System.exit(-1);
      }
    }
  }

  public static void failStop(Throwable th) {
    failStop("fail stop.", th);
  }

}
