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

import java.util.LinkedList;
import java.util.List;

public class Executor extends Thread {
  List<Runnable> queue;

  public Executor(String name) {
    super(name);
    queue = new LinkedList<Runnable>();
  }

  public void run() {
    Runnable r;
    while (true) {
      synchronized (queue) {
        while (queue.size() == 0)
          sleep();
        r = queue.remove(0);
      }
      r.run();
    }
  }

  public void post(Runnable r) {
    synchronized (queue) {
      queue.add(r); // at the end…
      queue.notify();
    }
  }

  private void sleep() {
    try {
      queue.wait();
    } catch (InterruptedException ex) {
      // nothing to do here.
    }
  }

  public static void check() {
    Thread thread = Thread.currentThread();
    if (!(thread instanceof Executor)) {
      Panic.failStop("Executor: wrong thread");
    }
      
  }
}
