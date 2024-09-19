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

import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Logger {
  boolean m_silent;
  String m_name;
  List<String> m_chunks;
  PrintStream m_ps;

  public Logger(String name, boolean silent, PrintStream ps) {
    m_ps = ps;
    m_silent = silent;
    m_name = "[" + name + "] ";
    m_chunks = new LinkedList<String>();
  }

  public synchronized void log(String s) {
    if (m_silent)
      return;
    m_chunks.add(s);
  }

  public synchronized void logln(String l) {
    if (m_silent)
      return;
    synchronized (m_ps) {
      m_ps.print(m_name);
      Iterator<String> i = m_chunks.iterator();
      while (i.hasNext()) {
        String s = i.next();
        m_ps.print(s);
      }
      m_ps.println(l);
    }
    m_chunks = new LinkedList<String>();
  }

}
