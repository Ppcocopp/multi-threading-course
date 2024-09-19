package info5.sar.queues;

import java.nio.ByteBuffer;

import info5.sar.channels.Channel;
import info5.sar.channels.DisconnectedException;

/**
 * Implementation of {@link MessageQueue}
 */
public class CMessageQueue extends MessageQueue {

	// The Channel to layout
	private Channel channel;
	// The parent QueueBroker
	private QueueBroker broker;
	// Objects only here for synchronization purpose
	private Object lock_in = new Object(), lock_out = new Object();

	/*
	 * Set the channel field with the given channel
	 * Set the broker field with the given broker
	 */
	/**
	 * Creates a fully-connected MessageQueue
	 * @param channel : the Channel to layout
	 * @param broker : the parent QueueBroker
	 */
	public CMessageQueue(Channel channel, QueueBroker broker) {
		this.channel = channel;
		this.broker = broker;
	}

	/*
	 * In a synchronized block on the lock_out object :
	 * | create a byte array containing the size of the message and the message itself exacly using the intToByteArray() and the concatArray() method
	 * | set a new counter to 0
	 * | while the counter is inferior to the length of the new array :
	 * | - increment the counter by the returned value of write() of the channel field
	 * | - the write() call should send the created array, the offset is the counter and the length to send is the total length of the array minus the counter
	 * | - if a DisconnectedException is thrown during the previous statement, catch it, close this connection and throw a ClosedException
	 */
	@Override
	public void send(byte[] bytes, int offset, int length) throws ClosedException {
		synchronized (lock_out) {
			// the size of the message on 4 bytes concatenated with the message
			byte[] message = concatArray(intToByteArray(length), 0, Integer.BYTES, bytes, offset, length);
			int byteSent = 0;
			while (byteSent < message.length) {
				try {
					byteSent += channel.write(message, byteSent, message.length - byteSent);
				} catch (DisconnectedException e) {
					this.close();
					throw new ClosedException(this.toString()+" send : Closed");
				}
			}
		}
	}

	/*
	 * In a synchronized block on the lock_in object :
	 * | initialize a new byte array the size of a integer
	 * | set a new counter to 0
	 * | while the counter is inferior to the size of the byte array :
	 * | - increment the counter by the returned value of read() of the channel field
	 * | - the read() call should write on the created array, the offset is the counter and the length to send is the length of the array minus the counter
	 * | - if a DisconnectedException is thrown during the previous statement, catch it, close this connection and throw a ClosedException
	 * | create a new variable 'length' which is the integer interpretation of the previous byte array (using byteArrayToInt())
	 * | initialize a new byte array the size of 'length'
	 * | set a new counter to 0
	 * | while the new counter is inferior to the new array length :
	 * | - increment the new counter by the returned value of read() of the channel field
	 * | - the read() call should write on the new created array, the offset is the new counter and the length to send is the length of the new array minus the new counter
	 * | - if a DisconnectedException is thrown during the previous statement, catch it, close this connection and throw a ClosedException
	 * | return the new array
	 */
	@Override
	public byte[] receive() throws ClosedException {
		synchronized (lock_in) {
			// receiving the size of the message
			byte[] messageLength = new byte[Integer.BYTES];
			int lengthIndex = 0;
			while (lengthIndex < messageLength.length) {
				try {
					lengthIndex += channel.read(messageLength, lengthIndex, messageLength.length - lengthIndex);
				} catch (DisconnectedException e) {
					this.close();
					throw new ClosedException();
				}
			}
			int length = byteArrayToInt(messageLength);
			// receiving the message
			byte[] message = new byte[length];
			int messageIndex = 0;
			while (messageIndex < length) {
				try {
					messageIndex += channel.read(message, messageIndex, length - messageIndex);
				} catch (DisconnectedException e) {
					this.close();
					throw new ClosedException();
				}
			}
			return message;
		}
	}

	/*
	 * Call disconnect() of the channel field
	 */
	@Override
	public void close() {
		channel.disconnect();
	}

	/*
	 * Call disconnected() of the channel field
	 */
	@Override
	public boolean closed() {
		return channel.disconnected();
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
		if (a == null || offset_a < 0 || length_a < 0 || a.length < offset_a + length_a)
			throw new IllegalArgumentException("Illegal arguments for byte[] concatenation");
		if (b == null || offset_b < 0 || length_b < 0 || b.length < offset_b + length_b)
			throw new IllegalArgumentException("Illegal arguments for byte[] concatenation");
		byte[] result = new byte[length_a + length_b];
		int index = 0;
		for (int i = offset_a; i < length_a; i++) {
			result[index] = a[i];
			index++;
		}
		for (int i = offset_b; i < length_b; i++) {
			result[index] = b[i];
			index++;
		}
		return result;
	}

	/**
	 * @param i : the integer to convert
	 * @return The bytes constituting the given integer as a byte array of a fixed size : 4
	 */
	private byte[] intToByteArray(int i) {
		ByteBuffer buffer = ByteBuffer.wrap(new byte[Integer.BYTES]);
		buffer.putInt(i);
		return buffer.array();
	}

	/**
	 * @param array : the array to interpret
	 * @return The integer interpretation of the array
	 */
	private int byteArrayToInt(byte[] array) {
		ByteBuffer buffer = ByteBuffer.wrap(array);
		return buffer.getInt();
	}

	/*
	 * return the broker field
	 */
	@Override
	public QueueBroker broker() {
		return this.broker;
	}
	
	@Override
	public String toString() {
		return "CMessageQueue[" + broker.getName() + ":" + channel.getPort() + "]-[" + channel.getRemoteName() + ":"
				+ channel.getPort() + "]";
	}
}
