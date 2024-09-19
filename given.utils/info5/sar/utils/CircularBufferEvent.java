package info5.sar.utils;

import java.util.ArrayList;
import java.util.List;


/* New class of a CircularBufferEvent. This one has 2 listeners for each channel : 
 * One to know if there is something to read and an other one to know if there is something to write 
 * and an executor */

public class CircularBufferEvent {
	int m_tail, m_head;
	byte m_bytes[];
	private InListener inListener;
	private OutListener outListener;
	private Executor executor;

	
/* Constructor with the capacity (=the length of the byte) and the executor */
	public CircularBufferEvent(int capacity, Executor executor) {
		m_bytes = new byte[capacity];
		m_tail = m_head = 0;
		this.executor = executor;
	}
	
/* Constructor with the capacity, the executor and the Inlistener */
	public CircularBufferEvent(int capacity, Executor executor, InListener inListener) {
		m_bytes = new byte[capacity];
		m_tail = m_head = 0;
		this.executor = executor;
		this.inListener = inListener;
	}
	
	
/* Constructor with the capacity, the executor and the Outlistener */
	public CircularBufferEvent(int capacity, Executor executor, OutListener outListener) {
		m_bytes = new byte[capacity];
		m_tail = m_head = 0;
		this.executor = executor;
		this.outListener = outListener;
	}

	/**
	 * @return true if this buffer is full, false otherwise
	 */
	public boolean full() {
		int next = (m_head + 1) % m_bytes.length;
		return (next == m_tail);
	}

	/**
	 * @return true if this buffer is empty, false otherwise
	 */
	public boolean empty() {
		return (m_tail == m_head);
	}

	/**
	 * @param b: the byte to push in the buffer
	 * @return the next available byte
	 * @throws an IllegalStateException if full.
	 */
	public void push(byte b) {
		boolean wasEmpty = empty();
		int next = (m_head + 1) % m_bytes.length;
		if (next == m_tail)
			throw new IllegalStateException();
		m_bytes[m_head] = b;
		m_head = next;
		if(wasEmpty && inListener!=null) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					inListener.bytesAvailable();
				}
			};
			executor.post(r);
		}
	}

	/**
	 * @return the next available byte
	 * @throws an IllegalStateException if empty.
	 */
	public byte pull() {
		boolean wasFull = full();
		if (m_tail == m_head)
			throw new IllegalStateException();
		int next = (m_tail + 1) % m_bytes.length;
		byte bits = m_bytes[m_tail];
		m_tail = next;
		if(wasFull && outListener!=null) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					outListener.spaceFreed();
				}
			};
			executor.post(r);
		}
		return bits;
	}
	
	public interface InListener {
		public void bytesAvailable();
	}
	
	public interface OutListener {
		public void spaceFreed();
	}
}
