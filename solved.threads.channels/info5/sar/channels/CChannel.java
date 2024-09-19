package info5.sar.channels;

import info5.sar.utils.CircularBuffer;

/**
 * Implementation of {@link Channel}
 */
public class CChannel extends Channel {

	// Input and output buffers
	private CircularBuffer in, out;
	// Disconnection state flag
	private boolean disconnected = false;
	// The Channel you are communicating with ('distant' channel)
	private CChannel linkedChannel;
	// Communication port
	private int port;

	/*
	 * Call the super() constructor with the given broker.
	 * Store the given port in the field.
	 * Initialize the in and out buffers in field with two different CircularBuffer
	 */
	/**
	 * Creates a non-fully connected Channel
	 * 
	 * @param broker : parent Broker
	 * @param port   : communication port
	 * @see {@link Channel#Channel(Broker) Channel(Broker)}
	 */
	protected CChannel(Broker broker, int port) {
		super(broker);
		this.port = port;
		this.in = new CircularBuffer(256);
		this.out = new CircularBuffer(256);
	}

	/*
	 * Call the super() constructor.
	 * Store the given port in the field.
	 * Store the given Channel in the field.
	 * Call the setLinkedChannel() method of the given Channel with this
	 * Store the in buffer of the given channel in your out field.
	 * Store the out buffer of the given channel in your in field.
	 */
	/**
	 * Creates a fully connected Channel.<br>
	 * Makes the 'distant' Channel fully connected.
	 * 
	 * @param broker  : parent Broker
	 * @param port    : communication port
	 * @param channel : 'distant' Channel
	 */
	protected CChannel(Broker broker, int port, CChannel channel) {
		super(broker);
		this.port = port;
		this.linkedChannel = channel;
		channel.setLinkedChannel(this);
		this.in = channel.getOutBuffer();
		this.out = channel.getInBuffer();
	}

	/*
	 * Throw a IllegalStateException if this is not linked
	 * Throw a IllegalArgumentException if either :
	 * - bytes is null
	 * - offset or length are negative
	 * - offset+length is superior to the length of the bytes array
	 * Throw a DisconnectedException if disconnected
	 * Set a new counter to 0;
	 * In a synchronized block on the in buffer object :
	 * | while the in buffer is empty : 
	 * | - if the 'distant' channel is disconnected, disconnect and throw a DisconnectedException
	 * | - wait() on the in buffer object and do nothing if a InterruptedException occurs
	 * | while he counter is inferior to the length :
	 * | - set the array value at the index offset+counter with the returned value of pull() of the in buffer.
	 * | - increment by 1 the counter.
	 * | - if one of the last two statements throws a IllegalStateException, break the while loop
	 * | notify all waiting threads on the in buffer object.
	 * return the counter value
	 */
	@Override
	public int read(byte[] bytes, int offset, int length) throws DisconnectedException {
		// checking if we can use the method
		if (!this.isLinked())
			throw new IllegalStateException("CChannel[" + broker.getName() + ":" + port + "] read : not linked");
		if (bytes == null || offset < 0 || length < 0 || offset + length > bytes.length)
			throw new IllegalArgumentException(this.toString() + " read : Illegal arguments");
		if (disconnected())
			throw new DisconnectedException(this.toString() + " read : Disconnected channel");
		int readBytes = 0;
		synchronized (in) {
			// wait for the first byte
			while (in.empty()) {
				if (linkedChannel.disconnected()) {
					disconnect();
					throw new DisconnectedException(this.toString() + " read : Linked channel disconnected");
				}
				try {
					in.wait();
				} catch (InterruptedException e) {
					// Nothing to do here
				}
			}
			// reading bytes
			while (readBytes < length) {
				try {
					bytes[offset + readBytes] = in.pull();
					readBytes++;
				} catch(IllegalStateException e) {
					break;
				}
				
			}
			in.notifyAll();
		}
		return readBytes;
	}

	/*
	 * Throw a IllegalStateException if this is not linked
	 * Throw a IllegalArgumentException if either :
	 * - bytes is null
	 * - offset or length are negative
	 * - offset+length is superior to the length of the bytes array
	 * Throw a DisconnectedException if disconnected
	 * Set a new counter to 0;
	 * In a synchronized block on the out buffer object :
	 * | - if the 'distant' channel is disconnected and the in buffer is empty, disconnect and throw a DisconnectedException
	 * | while the out buffer is full : 
	 * | - if the 'distant' channel is disconnected :
	 * | - - if the in buffer is empty disconnect and throw a DisconnectedException else return the counter value
	 * | - else :
	 * | - wait() on the out buffer object and do nothing if a InterruptedException occurs
	 * | while the counter is inferior to the length :
	 * | - push() on the out buffer the value of the array at the index offset+counter
	 * | - increment by 1 the counter.
	 * | - if one of the last two statements throws a IllegalStateException, break the while loop
	 * | notify all waiting threads on the out buffer object.
	 * return the counter value
	 */
	@Override
	public int write(byte[] bytes, int offset, int length) throws DisconnectedException {
		// checking if we can use the method
		if (!this.isLinked())
			throw new IllegalStateException("CChannel[" + broker.getName() + ":" + port + "] read : not linked");
		if (bytes == null || offset < 0 || length < 0 || offset + length > bytes.length)
			throw new IllegalArgumentException(this.toString() + " write : Illegal arguments");
		if (disconnected())
			throw new DisconnectedException(this.toString() + " write : Disconnected channel");
		int writtenBytes = 0;
		synchronized (out) {
			if (linkedChannel.disconnected() && in.empty()) {
				disconnect();
				throw new DisconnectedException(this.toString() + " read : Linked channel disconnected");
			}
			// wait for space
			while (out.full()) {
				if (linkedChannel.disconnected()) {
					disconnect();
					throw new DisconnectedException(this.toString() + " read : Linked channel disconnected");
				} else {
					try {
						out.wait();
					} catch (InterruptedException e) {
						// Nothing to do here
					}
				}
			}
			// writing bytes
			while (writtenBytes < length) {
				try {
					out.push(bytes[writtenBytes + offset]);
					writtenBytes++;
				} catch(IllegalStateException e) {
					break;
				}
				
			}
			out.notifyAll();
		}
		return writtenBytes;
	}

	/*
	 * Set the disconnected flag to true.
	 * In a synchronized block on the in buffer object, notify all waiting threads on the in buffer.
	 * In a synchronized block on the out buffer object, notify all waiting threads on the out buffer.
	 */
	@Override
	public void disconnect() {
		this.disconnected = true;
		synchronized (in) {
			in.notifyAll();
		}
		synchronized (out) {
			out.notifyAll();
		}
	}

	/*
	 * return the value of the disconnected flag
	 */
	@Override
	public synchronized boolean disconnected() {
		return this.disconnected;
	}

	/*
	 * return the in buffer in field
	 */
	public CircularBuffer getInBuffer() {
		return this.in;
	}

	/*
	 * return the out buffer in field
	 */
	public CircularBuffer getOutBuffer() {
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

	/*
	 * return the value of the getName() method of the result of getBroker() of the linkedChannel
	 */
	@Override
	public String getRemoteName() {
		return linkedChannel.getBroker().getName();
	}
	
	/**
	 * Return the value of the port field
	 */
	@Override
	public int getPort() {
		return port;
	}
	
	@Override
	public String toString() {
		return "CChannel[" + broker.getName() + ":" + port + "]-[" + linkedChannel.broker.getName() + ":"
				+ linkedChannel.port + "]";
	}

}
