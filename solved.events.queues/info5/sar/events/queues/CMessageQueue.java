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
package info5.sar.events.queues;

import java.nio.ByteBuffer;
import java.util.Arrays;

import info5.sar.channels.DisconnectedException;
import info5.sar.events.channels.Channel;
import info5.sar.events.channels.Channel.ReadListener;
import info5.sar.events.channels.Channel.WriteListener;
import info5.sar.events.queues.events.MessageQueue;
import info5.sar.events.queues.events.QueueBroker;
import info5.sar.utils.Executor;

/**
 * This is for the full event-oriented implementation, 
 * using a single event pump (Executor).
 * You must specify, design, and code the event-oriented
 * version of the channels and their brokers.
 */
public class CMessageQueue extends MessageQueue {
	
	// payload constants
	public static final byte[] UNBINDING_QUEUE_PAYLOAD = {0}, REGULAR_QUEUE_PAYLOAD = {1};
	// the channel to layout
	private Channel channel;
	// the event executor pump
	private Executor executor;
	// the parent broker
	private QueueBroker broker;
	// the stored listener
	private Listener listener;
	
	/*
	 * Set the channel field with the given channel
	 * Set the executor field with the given executor
	 * Set the broker field with the given broker
	 * Start the worker_writer  thread field 
	 */
	public CMessageQueue(Channel channel, QueueBroker broker, Executor executor) {
		this.channel = channel;
		this.executor = executor;
		this.broker = broker;
	}

	/* return the broker */
  @Override
  public QueueBroker broker() {
    return this.broker;
  }

  
  /* Set the listener to synchronized and check if this is the first time the listener has been set
   * if firstTime : 
   * -- The method startReadMessage() is started
   *   */
  @Override
  public synchronized void setListener(Listener l) {
	  boolean firstTime = this.listener==null;
	  this.listener = l;
	  if(firstTime) {
		  startReadMessage();
	  }
  }
  
  /*
   *Create a new byte array the size of 2 integers
   *Create a new read listener with in its read() method :
   *- If the first part of the byte array interpreted as an int + the length of the read bytes array equals the size of a int :
   *- - call the readMessage() method with the second part of the initial byte array interpreted as a int
   *- Else :
   *- - retrieve the previous index encoded in the first part of the initial byte array
   *- - add to it the size of the newly read byte array
   *- - convert this index to a new byte array and replace the first part of the initial byte array with this new one
   *- - try to read the rest of the message and call the close() method if a DisconnectedException is caught
   *Call the read() method of the channel to read the size of a int with an offset the same size.
   *If a DisconnectedException is caught, call the close() method
   */
  /**
   * Used to read the size of a message and call {@link #readMessage(int)} when done.
   */
  private void startReadMessage() {
	  byte[] messageSize = new byte[Integer.BYTES*2]; // messageSize[] = [reading index (int)] + [message size (int)]
	  ReadListener listenerSize = new ReadListener() {
		@Override
		public void read(byte[] bytes) {
			if(byteArrayToInt(Arrays.copyOf(messageSize, Integer.BYTES))+bytes.length==Integer.BYTES) {
				readMessage(byteArrayToInt(Arrays.copyOfRange(messageSize, Integer.BYTES, messageSize.length)));
			}else {
				int index = byteArrayToInt(Arrays.copyOf(messageSize, Integer.BYTES))+bytes.length;
				byte[] indexArray = intToByteArray(index);
				for(int y=0; y<indexArray.length; y++) messageSize[y] = indexArray[y];
				try {
					channel.read(messageSize, index+Integer.BYTES, Integer.BYTES-index, this);
				} catch (DisconnectedException e) {
					close();
				}
			}
		}
	  };
	  try {
		channel.read(messageSize, Integer.BYTES, Integer.BYTES, listenerSize);
	  } catch (DisconnectedException e) {
		close();
	  }
  }
  
  /*
   *Create a new byte array the size of an integers + the given size
   *Create a new read listener with in its read() method :
   *- If the first part of the byte array interpreted as an int + the length of the read bytes array equals the size of a int :
   *- - create a new Runnable calling the received() method of the listener field in its run() method and post it to the executor
   *- - call the startReadMessage() method
   *- Else :
   *- - retrieve the previous index encoded in the first part of the initial byte array
   *- - add to it the size of the newly read byte array
   *- - convert this index to a new byte array and replace the first part of the initial byte array with this new one
   *- - try to read the rest of the message and call the close() method if a DisconnectedException is caught
   *Call the read() method of the channel to read the size of a int with an offset the same size.
   *If a DisconnectedException is caught, call the close() method
   */
  /**
   * Reads a message of the given size and calls received() method of the listener field when done
   * @param size : the size of the message to read
   */
  private void readMessage(int size) {
	  byte[] message = new byte[Integer.BYTES+size]; // message[] = [reading index (int)] + [message]
	  ReadListener listenerSize = new ReadListener() {
		@Override
		public void read(byte[] bytes) {
			if(byteArrayToInt(Arrays.copyOf(message, Integer.BYTES))+bytes.length==size) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						listener.received(Arrays.copyOfRange(message, Integer.BYTES, message.length));
					}
				};
				executor.post(r);
				startReadMessage();
			}else {
				int index = byteArrayToInt(Arrays.copyOf(message, Integer.BYTES))+bytes.length;
				byte[] indexArray = intToByteArray(index);
				for(int y=0; y<indexArray.length; y++) message[y] = indexArray[y];
				try {
					channel.read(message, index+Integer.BYTES, size-index, this);
				} catch (DisconnectedException e) {
					close();
				}
			}
		}
	  };
	  try {
		channel.read(message, Integer.BYTES, size, listenerSize);
	  } catch (DisconnectedException e) {
		close();
	  }
  }

  /*
   * Create a new byte array which is the concatenation of the byte array interpretation of the size of the given byte array and the given byte array itself
   * Create a new write listener with in its written() method :
   * - if written is different from length :
   * - - try to call the write() method of the channel to write the rest of the message 
   * - - call the close() method if a DisconnectedException is caught
   * Try to call the write() method of the channel to write the whole new byte array
   * Call the close() method if a DisconnectedException is caught
   * return true
   */
  @Override
  public synchronized boolean send(byte[] bytes) {
	  byte[] size = intToByteArray(bytes.length);
	  byte[] message = concatArray(size, 0, size.length, bytes, 0, bytes.length);
	  WriteListener writeListener = new WriteListener() {
		@Override
		public void written(byte[] bytes, int offset, int length, int written) {
			if(written!=length) {
				try {
					channel.write(bytes, offset+written, length-written, this);
				} catch (DisconnectedException e) {
					close();
				}
			}
		}
	  };
      try {
		channel.write(message, 0, message.length, writeListener);
	} catch (DisconnectedException e) {
		close();
	}
      return true;
  }

  /*
   * If the channel is still connected :
   * - disconnect it
   * If the listener is not null :
   * - create a new Runnable which calls the closed() method of the listener field in its run() method
   * - post this runnable to the executor
   */
  @Override
  public void close() {
		if (!channel.disconnected())
			channel.disconnect();
		if (listener != null) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					listener.closed();
				}
			};
			executor.post(r);
		}
  }

  
  /* Calling the disconnected() method on the channel 
   * Return true if the channel is disconnected() or false if not */
  @Override
  public boolean closed() {
    return channel.disconnected();
  }

  
  /* Return the name of the remote channel */
  @Override
  public String getRemoteName() {
    return this.channel.getRemoteName();
  }
  
    /**
	 * Converts a byte array to its integer interpretation
	 * @param array : the byte array to convert
	 * @return The interpreted integer
	 */
	private int byteArrayToInt(byte[] array) {
		ByteBuffer buffer = ByteBuffer.wrap(array);
		return buffer.getInt();
	}
	
	/**
	 * Converts an integer to a byte array
	 * @param i : the integer to convert
	 * @return : The byte array interpretation
	 */
	private byte[] intToByteArray(int i) {
		ByteBuffer buffer = ByteBuffer.wrap(new byte[Integer.BYTES]);
		buffer.putInt(i);
		return buffer.array();
	}
	
	/**
	 * Reads the size of the payload and calls the {@link #readPayload(int)} method when done.
	 * @param listener : the listener to call when the payload is read
	 */
	public void startReadPayload(ReadListener listener) {
		byte[] messageSize = new byte[Integer.BYTES*2]; // messageSize[] = [reading index (int)] + [message size (int)]
		  ReadListener listenerSize = new ReadListener() {
			@Override
			public void read(byte[] bytes) {
				if(byteArrayToInt(Arrays.copyOf(messageSize, Integer.BYTES))+bytes.length==Integer.BYTES) {
					readPayload(byteArrayToInt(Arrays.copyOfRange(messageSize, Integer.BYTES, messageSize.length)), listener);
				}else {
					int index = byteArrayToInt(Arrays.copyOf(messageSize, Integer.BYTES))+bytes.length;
					byte[] indexArray = intToByteArray(index);
					for(int y=0; y<indexArray.length; y++) messageSize[y] = indexArray[y];
					try {
						channel.read(messageSize, index+Integer.BYTES, Integer.BYTES-index, this);
					} catch (DisconnectedException e) {
						close();
					}
				}
			}
		  };
		  try {
			channel.read(messageSize, Integer.BYTES, Integer.BYTES, listenerSize);
		  } catch (DisconnectedException e) {
			close();
		  }
	}

	/**
	 * Reads a payload and call the read() method of the listener when done
	 * @param size : the size of the payload
	 * @param listener : the listener to call when done
	 */
	private void readPayload(int size, ReadListener listener) {
		  byte[] message = new byte[Integer.BYTES+size]; // message[] = [reading index (int)] + [message]
		  ReadListener listenerSize = new ReadListener() {
			@Override
			public void read(byte[] bytes) {
				if(byteArrayToInt(Arrays.copyOf(message, Integer.BYTES))+bytes.length==size) {
					Runnable r = new Runnable() {
						@Override
						public void run() {
							listener.read(Arrays.copyOfRange(message, Integer.BYTES, message.length));
						}
					};
					executor.post(r);
				}else {
					int index = byteArrayToInt(Arrays.copyOf(message, Integer.BYTES))+bytes.length;
					byte[] indexArray = intToByteArray(index);
					for(int y=0; y<indexArray.length; y++) message[y] = indexArray[y];
					try {
						channel.read(message, index+Integer.BYTES, Integer.BYTES-index, this);
					} catch (DisconnectedException e) {
						close();
					}
				}
			}
		  };
		  try {
			channel.read(message, Integer.BYTES, size, listenerSize);
		  } catch (DisconnectedException e) {
			close();
		  }
	  }

	/**
	 * <pre>Exemple : concatArray([0,1,2,3,4], 1, 3, [5,6,7,8,9], 0, 4) <br>	returns [1,2,3,5,6,7,8]</pre>
	 * @param a : the first array
	 * @param offset_a : the starting index of the first array
	 * @param length_a : the number of bytes to copy from the first array
	 * @param b : the second array
	 * @param offset_b : the starting index of the second array
	 * @param length_b : the number of bytes to copy from the second array
	 * @return The concatenated 2 arrays
	 */
	private byte[] concatArray(byte[] a, int offset_a, int length_a, byte[] b, int offset_b, int length_b) {
		// exception si les arguments ne sont pas valide
		if(a==null || offset_a<0 || length_a<0 || a.length<offset_a+length_a)
			throw new IllegalArgumentException("Illegal arguments for byte[] concatenation");
		if(b==null || offset_b<0 || length_b<0 || b.length<offset_b+length_b)
			throw new IllegalArgumentException("Illegal arguments for byte[] concatenation");
		// concaténation du tableau a et b
		byte[] result = new byte[length_a+length_b];
		int index = 0;
		for(int i=offset_a; i<length_a; i++) {
			result[index] = a[i];
			index++;
		}
		for(int i=offset_b; i<length_b; i++) {
			result[index] = b[i];
			index++;
		}
		return result;
	}
	
}
