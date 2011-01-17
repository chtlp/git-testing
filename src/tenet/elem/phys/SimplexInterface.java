package tenet.elem.phys;

import tenet.command.*;
import tenet.constant.Status;
import tenet.core.Config;
import tenet.core.Log;
import tenet.core.Simulator;
import tenet.elem.Agent;
import tenet.elem.ip.IPAddr;

public class SimplexInterface extends Interface {

	private Queue<byte[]> m_queue;
	private SimplexLink m_link;
	private int m_status;
	private int m_direction;

	public SimplexInterface(int direction, IPAddr addr) {
		super(addr);

		m_direction = direction;
		m_status = Status.UP;
		m_link = null;

		m_queue = new QueueDropTail(
				Config.getInteger("system.DefaultMaxQueueLength"));
		m_queue.attach(this);
	}

	public SimplexInterface(int direction, IPAddr addr, int bandwidth) {
		super(addr, bandwidth);

		m_direction = direction;
		m_status = Status.UP;
		m_link = null;

		m_queue = new QueueDropTail(
				Config.getInteger("system.DefaultMaxQueueLength"));
		m_queue.attach(this);
	}

	public void attach(Node node) {
		m_node = node;
	}

	public void attach(Link link, boolean inheritBandwidth) {
		if (!(link instanceof SimplexLink)) {
			Log.fatal("You can only attach a SimplexLink to a SimplexInterface!");
			System.exit(1);
		}

		SimplexLink simplexlink = (SimplexLink) link;

		switch (m_direction) {
		case Interface.RECEIVER:
			simplexlink.setIncoming(this);
			m_link = simplexlink;
			break;

		case Interface.SENDER:
			simplexlink.setOutgoing(this);
			m_link = simplexlink;
			break;
		}

		// TODO: to be determined
		if (inheritBandwidth)
			m_bandwidth = link.getBandwidth();
	}

	public void attach(Queue<byte[]> queue) {
		m_queue = queue;
		m_queue.attach(this);
	}

	public int getType() {
		return m_direction;
	}

	public Node getNode() {
		return m_node;
	}

	public boolean canSend(IPAddr destination, int length) {
		return (m_status == Status.UP && !m_queue.isFull(length));
	}

	/**
	 * send puts a packet to be sent in the send queue of this interface and
	 * schedules a call to the update() method. You are advised to use the
	 * canSend function to check if the packet can be sent first, otherwise it
	 * might be dropped.
	 * 
	 * @param packet
	 *            the packet to be sent
	 */
	public void send(byte[] packet) {
		if (m_status == Status.DOWN)
			return;
		if (m_direction != Interface.SENDER)
			Log.fatal("Trying to send a packet from a receiver interface");

		// Enqueue the packet in the send queue
		m_queue.enqueue(packet);

		// Schedule a call to the update function
		Simulator.getInstance().schedule(
				new ElementUpdateCommand(this, Simulator.getInstance()
						.getTime()
						+ Config.getDouble("system.DelayIfaceSendUpdate")));

	}

	public byte[] read(int unique_id) {
		if (m_status == Status.DOWN)
			return null;
		if (m_direction != Interface.RECEIVER)
			Log.fatal("Trying to read a packet from a sender interface");

		byte[] packet = m_queue.dequeue();
		if (packet == null) {
			Log.warn("No packets in queue when read() was called - "
					+ "Natural Overflow?");
			return null;
		}

		return packet;
	}

	public void dump() {
		System.out.println("SimplexInterface: " + m_addr);
		System.out.println("Status: " + m_status);
		System.out.println("Direction: " + m_direction);
	}

	public void indicate(int status, Object indicator) {
		// Only links may indicate to interfaces!
		if (!(indicator instanceof Link))
			Log.fatal("Interface received an indication from a non-Link");

		// We are clear to send, schedule a call to update
		if (m_status == Status.UP && (status & Agent.READY_TO_SEND) != 0)
			Simulator.getInstance().schedule(
					new ElementUpdateCommand(this, Simulator.getInstance()
							.getTime()
							+ Config.getDouble("system.DelayIfaceSendUpdate")));

		// A packet is waiting for collection
		if ((status & Agent.PACKET_AVAILABLE) != 0) {
			SimplexLink l = (SimplexLink) indicator;
			byte[] packet = (byte[]) l.read(0);

			if (m_status == Status.DOWN)
				return;
			m_queue.enqueue(packet);

			// Indicate to IP handler
			m_handler.indicate(Agent.PACKET_AVAILABLE, this);
		}
	}

	/**
	 * update will do either of two things: If the interface is a SENDER, it
	 * will try to take packets off the queue and put them on the link.
	 * Conversely, if it is a receiver, it will take packets off the link and
	 * stick them in the queue. WRONG.
	 */
	public void update() {
		Log.debug(debugFlag, "Interface update");

		if (m_status == Status.DOWN)
			return;
		if (m_link == null) {
			Log.fatal("You are trying to send packets from an iface without a link!");
			System.exit(1);
		}

		if (m_direction == Interface.SENDER && m_link.canSend()) {
			byte[] packet = m_queue.dequeue();

			if (packet != null) {
				m_link.send(packet);

				// A packet is gone so the queue can't be full anymore. Indicate
				// this to the IP handler.
				m_handler.indicate(Agent.READY_TO_SEND, this);
			} else
				m_handler.indicate(Agent.READY_TO_SEND, this);
		}

	}

	@Override
	public void setStatus(int status) {
		if (m_status == Status.UP && status == Status.DOWN) {
			while (!m_queue.isEmpty())
				m_queue.dequeue();
		}
		m_status = status;
	}

	public static final char debugFlag = 'L';
}
