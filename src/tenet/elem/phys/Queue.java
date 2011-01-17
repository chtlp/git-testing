package tenet.elem.phys;

import tenet.elem.Element;

/**
 * Queue implements a queue that can be attached to an interface.
 */
public abstract class Queue<T> extends Element {

	protected Interface m_iface;
	protected int m_maxlength;
	protected int m_curlength;

	/**
	 * Create a new queue with the given maximum length (in bytes!).
	 * 
	 * @param length
	 *            maximum length of the queue in bytes, 0 if the queue is
	 *            infinite
	 */
	public Queue(int length) {
		m_maxlength = length;
		m_curlength = 0;
		m_iface = null;
	}

	public void attach(Interface iface) {
		m_iface = iface;
	}

	/**
	 * Return the maximum length of the queue in bytes. Returns 0 for an
	 * infinite queue.
	 * 
	 * @return the maximum queue length in bytes. 0 if the queue is infinite
	 */
	public int getMaximumLength() {
		return m_maxlength;
	}

	/**
	 * Check if the queue is full if a given number of bytes is to be added.
	 * 
	 * @param bytes
	 *            the number of bytes to be added
	 * @return true if the queue would be full after adding bytes (i.e., no
	 *         bytes could be added)
	 */
	public boolean isFull(int bytes) {
		if (m_maxlength == 0)
			return false;
		return m_curlength + bytes > m_maxlength;
	}

	public boolean isEmpty() {
		return m_curlength == 0;
	}

	/**
	 * Enqueue a packet in this queue, if it fits in. The routine fails silently
	 * if the packet does not fit it. If you have to know in advance if it will,
	 * use the isFull function first.
	 * 
	 * @param packet
	 *            the packet to enqueue
	 * @see isFull
	 */
	public abstract void enqueue(T packet);

	/**
	 * Dequeue a packet from this queue. Note that this does not mean the packet
	 * at the end of the queue is being dequeued. The queueing policy is decided
	 * by subclasses.
	 * 
	 * @return a dequeued packet that was removed from the queue
	 */
	public abstract T dequeue();

}
