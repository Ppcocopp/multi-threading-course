package info5.sar.mixed.queues;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import info5.sar.channels.Channel;
import info5.sar.channels.DisconnectedException;
import info5.sar.events.queues.mixed.MessageQueue;
import info5.sar.events.queues.mixed.QueueBroker;
import info5.sar.utils.Executor;


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
	// the automatic reader thread
	private Thread worker_reader;
	// the automatic sender thread
	private WorkerWriter worker_writer=new WorkerWriter();
	
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
		worker_writer.start();
	}

	/*
	 * Set the listener field with the given listener
	 * if the worker_reader field is not initialised :
	 * - set it with a new Thread and in its run() method :
	 * - - while the connection is not closed :
	 * - - - In a synchronized block on this object :
	 * - - - | create a new initialized byte array variable of the length of the encoding of an integer in bytes
	 * - - - | set a new counter to 0
	 * - - - | while the counter is inferior to the length of the array
	 * - - - | - increment the counter by the returned value of the read() method of the channel field.
	 * - - - | - this read() call should write on the newly created array, use the counter as offset and read the length of the array minus the counter bytes
	 * - - - | - if a DisconnectedException is thrown during the read() call, catch it, close the connection and break the while loop
	 * - - - | Create a new integer variable 'length' which is the integer interpretation of the byte array
	 * - - - | create a new initialized byte array the size of 'length" variable
	 * - - - | set a new counter to 0
	 * - - - | while the counter is inferior to the 'length' variable
	 * - - - | - increment the new counter by the returned value of the read() method of the channel field.
	 * - - - | - this read() call should write on the newly created array, use the new counter as offset and read the length of the new array minus the new counter bytes
	 * - - - | - if a DisconnectedException is thrown during the read() call, catch it, close the connection and break the while loop
	 * - - - | if the length of the new array is superior to 0 :
	 * - - - | - create a new Runnable calling the received() method of the listener field in its run() method
	 * - - - | - post this Runnable to the event executor pump in field
	 * - start the thread
	 */
	@Override
	public synchronized void setListener(Listener l) {
		this.listener = l;		
		if(worker_reader==null) {
			worker_reader = new Thread() {
				@Override
				public void run() {
					while(!closed()) {
						synchronized(this){
							// retrieving the message length
							byte[] messageLength = new byte[Integer.BYTES];
							int lengthIndex = 0;
							while(lengthIndex<messageLength.length) {
								try {
									lengthIndex += channel.read(messageLength, lengthIndex, messageLength.length-lengthIndex);
								} catch (DisconnectedException e) {
									close();
									break;
								}
							}
							// receiving the message
							int length = byteArrayToInt(messageLength);
							byte[] message = new byte[length];
							int messageIndex = 0;
							while(messageIndex<length) {
								try {
									messageIndex += channel.read(message, messageIndex, length-messageIndex);
								} catch (DisconnectedException e) {
									close();
									break;
								}
							}
							if(message.length>0) {
								Runnable r = new Runnable() {
									@Override
									public void run() {
										listener.received(message);
									}
								};
								executor.post(r);
							}
						}
					}
				}
			};
			worker_reader.start();
		}
	}
	

	/*
	 * If the field channel is still connected, disconnect it
	 * If the listener field is initialized :
	 * - create a new Runnable calling the closed() method of the listener field in its run() method
	 * - post this Runnable to the event executor pump in field
	 * kill the worker_writer field
	 */
	@Override
	public synchronized void close() {
		if(!channel.disconnected()) channel.disconnect();
		if(listener!=null) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					listener.closed();
				}
			};
			executor.post(r);
		}
		worker_writer.kill();
	}

	/*
	 * return the returned value of the disconnected() method of the channel field
	 */
	@Override
	public synchronized boolean closed() {
		return channel.disconnected();
	}

	/*
	 * Call the send() method of the worker_writer field with a copy of the given array
	 * Return true
	 */
	@Override
	public synchronized boolean send(byte[] bytes) {
		worker_writer.send(Arrays.copyOf(bytes, bytes.length));
		return true;
	}
	
	/*
	 * Return the returned value of the getRemoteName() method of the channel field
	 */
	@Override
	public String getRemoteName() {
		return this.channel.getRemoteName();
	}

	/*
	 * Return the broker field
	 */
	@Override
	public QueueBroker broker() {
		return this.broker;
	}
	
	/**
	 * This is a thread-safe blocking method
	 * @return The payload read
	 */
	public synchronized byte[] readPayload() {
		byte[] payloadSize = new byte[Integer.BYTES];
		int readPayloadSize = 0;
		while(readPayloadSize<Integer.BYTES) {
			try {
				readPayloadSize += channel.read(payloadSize, readPayloadSize, Integer.BYTES-readPayloadSize);
			} catch (DisconnectedException e) {
				return UNBINDING_QUEUE_PAYLOAD;
			}
		}
		int length = byteArrayToInt(payloadSize);
		byte[] payload = new byte[length];
		int readPayload = 0;
		while(readPayload<length) {
			try {
				readPayload += channel.read(payload, readPayload, length-readPayload);
			} catch (DisconnectedException e) {
				payload = UNBINDING_QUEUE_PAYLOAD;
				break;
			}
		}
		return payload;
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
	 * Working thread used to automatically send messages<br>
	 * This is thread-safe and FIFO
	 */
	public class WorkerWriter extends Thread {
		// the FIFO queue of messages to send
		private List<byte[]> queue = new ArrayList<>();
		// the alive flag
		private boolean alive = true;	
		
		@Override
		public void run() {
			while(alive) {
				if(queue.size()>0) {
					byte[] request = queue.get(0);
					byte[] message = concatArray(intToByteArray(request.length), 0, Integer.BYTES, request, 0, request.length);
					int byteSent = 0;
					while(byteSent<message.length) {
						try {
							byteSent += channel.write(message, byteSent, message.length-byteSent);
						} catch (DisconnectedException e) {
							close();
							break;
						}
					}
					if(byteSent==message.length)
						queue.remove(request);
						
				}else {
					synchronized(this) {
						try {
							wait();
						} catch (InterruptedException e) {
							// nothing to do here
						}
					}
				}
			}
		}
		
		/**
		 * Kills this thread
		 */
		public void kill() {
			alive = false;
			synchronized(this) {
				notify();
			}
		}
		
		/**
		 * Sends a message.
		 * This is a tread-safe FIFO method
		 * @param message : the message to send
		 */
		public synchronized void send(byte[] message) {
			queue.add(message);
			notify();
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

	}
}
