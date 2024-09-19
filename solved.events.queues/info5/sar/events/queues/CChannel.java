package info5.sar.events.queues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import info5.sar.channels.DisconnectedException;
import info5.sar.events.channels.Broker;
import info5.sar.events.channels.Channel;
import info5.sar.utils.CircularBufferEvent;
import info5.sar.utils.Executor;

public class CChannel extends Channel {

	// Input and output buffers
	private CircularBufferEvent in, out;
	// Disconnection state flag
	private boolean disconnected = false;
	// The Channel you are communicating with ('distant' channel)
	private CChannel linkedChannel;
	// Communication port
	private int port;
	// the event executor pump
	private Executor executor;
	
	// List of WriteReaquest and ReadRequest 
	private List<WriteRequest> writeRequests = new ArrayList<>();
	private List<ReadRequest> readRequests = new ArrayList<>();
	
	// Using to know if we are writing or reading on the channel
	private boolean writing=false, reading=false;

	/*
	 * Call the super() constructor with the given broker.
	 * Store the given port in the field.
	 * Initialize the in and out buffers in field with two different CircularBuffer : 
	 * 
	 * Synchronize readRequests and check if there is a new Request and if we are and if we're not already reading :
	 * --- if true : the executor works on the first request
	 * Synchronize writeRequests and check if there is a new Request and if we are and if we're not already writing :
	 * --- if true : the executor works on the first request
	 */
	/**
	 * Creates a non-fully connected Channel
	 * 
	 * @param broker : parent Broker
	 * @param port   : communication port
	 * @see {@link Channel#Channel(Broker) Channel(Broker)}
	 */
	protected CChannel(Broker broker, int port, Executor executor) {
		super(broker);
		this.port = port;
		this.in = new CircularBufferEvent(256, executor, new CircularBufferEvent.InListener() {
			@Override
			public void bytesAvailable() {
				synchronized(readRequests) {
					if(readRequests.size()>0 && !reading) {
						executor.post(readRequests.get(0));
						reading = true;
					}
				}
				
			}
		});
		this.out = new CircularBufferEvent(256, executor, new CircularBufferEvent.OutListener() {
			@Override
			public void spaceFreed() {
				synchronized(writeRequests) {
					if(writeRequests.size()>0 && !writing) {
						executor.post(writeRequests.get(0));
						writing = true;
					}
						
				}
			}
		});
		this.executor = executor;
	}

	/*
	 * Call the super() constructor.
	 * Store the given port in the field.
	 * Store the given Channel in the field.
	 * Call the setLinkedChannel() method of the given Channel with this
	 * Store the in buffer of the given channel in your out field.
	 * Store the out buffer of the given channel in your in field.
	 * Store the given executor in the field. 
	 */
	/**
	 * Creates a fully connected Channel.<br>
	 * Makes the 'distant' Channel fully connected.
	 * 
	 * @param broker  : parent Broker
	 * @param port    : communication port
	 * @param channel : 'distant' Channel
	 */
	protected CChannel(Broker broker, int port, CChannel channel, Executor executor) {
		super(broker);
		this.port = port;
		this.linkedChannel = channel;
		channel.setLinkedChannel(this);
		this.in = channel.getOutBuffer();
		this.out = channel.getInBuffer();
		this.executor = executor;
	}

	@Override
	public String getRemoteName() {
		return this.linkedChannel.getBroker().getName();
	}

	@Override
	public int getPort() {
		return this.port;
	}

	
	/*
	 * This method post a readRequest
	 * Checking if the channel is linked
	 * Checking if the arguments are right
	 * Checking if the channel is not disconnect
	 * Adding a new ReadRequest with the given arguments
	 * Synchronization on the list of the readRequests and if we are not already reading : 
	 * --- The executor works on the first request 
	 * --- The read flag changes to true
	 */
	@Override
	public void read(byte[] bytes, int offset, int length, ReadListener listener) throws DisconnectedException {
		// checking if we can use the method
		if (!this.isLinked())
			throw new IllegalStateException("CChannel[" + this.getBroker().getName() + ":" + port + "] read : not linked");
		if (bytes == null || offset < 0 || length < 0 || offset + length > bytes.length)
			throw new IllegalArgumentException(this.toString() + " read : Illegal arguments");
		if (disconnected())
			throw new DisconnectedException(this.toString() + " read : Disconnected channel");
		readRequests.add(new ReadRequest(bytes, offset, length, listener));
		synchronized(readRequests) {
			if(!reading) {
				executor.post(readRequests.get(0));
				reading = true;
			}
		}
	}

	
	/*
	 * This method post a writeRequest
	 * Checking if the channel is linked
	 * Checking if the arguments are right
	 * Checking if the channel is not disconnect
	 * Adding a new WriteRequest with the given arguments
	 * Synchronization on the list of the writeRequests and if we are not already writing : 
	 * --- The executor works on the first request 
	 * --- The write flag changes to true
	 */
	@Override
	public void write(byte[] bytes, int offset, int length, WriteListener listener) throws DisconnectedException {
		// checking if we can use the method
		if (!this.isLinked())
			throw new IllegalStateException("CChannel[" + this.getBroker().getName() + ":" + port + "] read : not linked");
		if (bytes == null || offset < 0 || length < 0 || offset + length > bytes.length)
			throw new IllegalArgumentException(this.toString() + " write : Illegal arguments");
		if (disconnected())
			throw new DisconnectedException(this.toString() + " write : Disconnected channel");
		writeRequests.add(new WriteRequest(bytes, offset, length, listener));
		synchronized(writeRequests) {
			if(!writing) {
				executor.post(writeRequests.get(0));
				writing = true;
			}
		}
	}
	
	/* The field disconnected changes to true */

	@Override
	public void disconnect() {
		this.disconnected = true;
	}

	/* 
	 * Return true if the channel is disconnected or false if not
	 */
	@Override
	public boolean disconnected() {
		return this.disconnected;
	}
	
	/*
	 * return the in buffer in field
	 */
	public CircularBufferEvent getInBuffer() {
		return this.in;
	}

	/*
	 * return the out buffer in field
	 */
	public CircularBufferEvent getOutBuffer() {
		return this.out;
	}

	/*
	 * set the linkedChannel field to this channel
	 */
	public void setLinkedChannel(CChannel channel) {
		this.linkedChannel = channel;
	}

	/*
	 * return true if the linkedChannel field is not null
	 */
	public boolean isLinked() {
		return this.linkedChannel != null;
	}
	
	/* Each WriteRequest is a Runnable
	 * Each WriteRequest has :
	 * -- the byte array to be written
	 * -- An offset
	 * -- the length
	 * -- A WriteListener
	 * 
	 * If the channel is disconnected the writeRequests list is cleaned
	 * If the outCircularBuffer is not full :
	 * -- if the remote channel is disconnected and there is nothing to read in the InCircularBuffer :
	 * -------- The channel is disconnected and the writeRequests list is cleaned
	 * -- As long as we can, we write in the outCircularBuffer
	 * -- A new runnable is created and the listener knows that a message has been written and knows the number of bytes written
	 * -- The executor post the runnable
	 * -- The request that has just been executed is removed from the list of WriteRequest
	 * -- If the outCircularBuffer is not full and if there is another writeRequest in the list
	 * -------- The executor post the first one of the list
	 * -- else the write flag changes to false
	 * else the write flag changes to false
	 */
	
	private class WriteRequest implements Runnable {
		byte[] bytes;
		int offset;
		int length;
		WriteListener listener;
		
		
		public WriteRequest(byte[] bytes, int offset, int length, WriteListener listener) {
			this.bytes = bytes;
			this.offset = offset;
			this.length = length;
			this.listener = listener;
		}
		
		@Override
		public void run() {
			if(disconnected()) {
				writeRequests.clear();
				return;
			}
			if(!out.full()) {
				if (linkedChannel.disconnected() && in.empty()) {
					disconnect();
					writeRequests.clear();
					return;
				}
				int writtenBytes = 0;
				while(!out.full() && writtenBytes<length && !disconnected()) {
					try {
						out.push(bytes[writtenBytes + offset]);
						writtenBytes++;
					} catch(IllegalStateException e) {
						break;
					}
				}
				final int b = writtenBytes;
				Runnable r = new Runnable() {
					@Override
					public void run() {
						listener.written(bytes, offset, length, b);
					}
				};
				executor.post(r);
				writeRequests.remove(this);
				if(!out.full() && writeRequests.size()>0) 
					executor.post(writeRequests.get(0));
				else
					synchronized(writeRequests) {
						writing = false;
					}
			} else {
				synchronized(writeRequests) {
					writing = false;
				}
			}
		}
	}
	
	
	
	/*
	 * Each ReadRequest is a Runnable
	 * Each ReadRequest has :
	 * -- the byte array to be read
	 * -- An offset
	 * -- the length
	 * -- A ReadListener
	 * 
	 * If the channel is disconnected the readRequests list is cleaned
	 * If the inCircularBuffer is not empty :
	 * -- As long as we can, we from the inCircularBuffer
	 * -- A new runnable is created and the listener knows that a message has been read and knows the number of bytes read
	 * -- The executor post the runnable
	 * -- The request that has just been executed is removed from the list of ReadRequest
	 * -- If the inCircularBuffer is empty and the remote channel is disconnected : 
	 * -------- The channel is disconnected and the readRequests list is cleaned
	 * -- If the inCircularBuffer is not empty and if there is another ReadRequest in the list
	 * -------- The executor post the first one of the list
	 * -- else the read flag changes to false
	 * else the read flag changes to false
	 */
	
	private class ReadRequest implements Runnable {
		byte[] bytes;
		int offset;
		int length;
		ReadListener listener;
		
		
		public ReadRequest(byte[] bytes, int offset, int length, ReadListener listener) {
			this.bytes = bytes;
			this.offset = offset;
			this.length = length;
			this.listener = listener;
		}
		
		@Override
		public void run() {
			if(disconnected()) {
				readRequests.clear();
				return;
			}
			if(!in.empty()) {
				int readBytes = 0;
				while (readBytes < length && !disconnected()) {
					try {
						bytes[offset + readBytes] = in.pull();
						readBytes++;
					} catch(IllegalStateException e) {
						break;
					}
				}
				final int b = readBytes;
				Runnable r = new Runnable() {
					@Override
					public void run() {
						listener.read(Arrays.copyOfRange(bytes, offset, offset+b));
					}
				};
				executor.post(r);
				readRequests.remove(this);
				
				if (in.empty() && linkedChannel.disconnected()) {
					disconnect();
					readRequests.clear();
					return;
				}
				if(!in.empty() && readRequests.size()>0)
					executor.post(readRequests.get(0));
				else
					synchronized(readRequests) {
						reading = false;
					}
			} else {
				synchronized(readRequests) {
					reading = false;
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return "CChannel[" + this.getBroker().getName() + ":" + port + "]-[" + linkedChannel.getBroker().getName() + ":"
				+ linkedChannel.port + "]";
	}

}
