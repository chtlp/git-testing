package tenet.elem.phys;

import java.util.LinkedList;
import java.util.Random;

import tenet.command.*;
import tenet.constant.Status;
import tenet.core.Log;
import tenet.core.Simulator;
import tenet.elem.Agent;

public class SimplexLink extends Link {

	// Incoming and outgoing interface. That is m_in is a RECEIVER and m_out
	// is a SENDER interface, i.e. outgoint means 'out of the node' not
	// 'out of the link'
	private Interface m_in = null, m_out = null;

	// The bandwidth of the link in bps
	private int m_bandwidth;

	// The propagation delay of the link in seconds
	private double m_delay;

	// Error rate (between 0-1, where 1 is 100% error). Probability that a
	// packet is corrupted.
	private double m_error;

	// m_cansend is false if someone is busy putting bits onto the wire.
	// The link will normally block (packet size/bandwidth) seconds if someone
	// puts a packet on it
	private boolean m_cansend;

	// Queue of packets on the link. The queue structure is just used for
	// convenience.
	private LinkedList<byte[]> m_packets;

	// A queue of the packets that have arrived and are waiting for collection.
	// This should normally contain only one packet
	private LinkedList<byte[]> m_arrived;

	// The current status of the link, either Status.UP or Status.DOWN.
	private int m_status;

	public SimplexLink(int bandwidth, double delay) {
		m_bandwidth = bandwidth;
		m_delay = delay;
		m_error = 0.0;
		m_cansend = true;
		m_packets = new LinkedList<byte[]>();
		m_arrived = new LinkedList<byte[]>();
		m_status = Status.UP;
	}

	public SimplexLink(int bandwidth, double delay, double error) {
		this(bandwidth, delay);
		m_error = error;
	}

	public void dump() {
		System.out.println("SimplexLink: Bandwidth " + m_bandwidth
				+ " bps, Delay " + m_delay + " seconds");
		System.out.println("Error rate: " + m_error);
	}

	Random rand = new Random(2000);
	/**
	 * Update will send one packet from the packet into the arrived list
	 * (because it always gets called when a packet arrival is due). It will
	 * then indicate to the incoming interface.
	 */
	public void update() {
		Log.debug(debugFlag, "Link Update");

		if (m_status == Status.DOWN)
			return;
		if (m_packets.size() == 0)
			return;

		// The packet at the end of the queue is the one that arrives first
		byte[] packet = m_packets.pollLast();

		// Play the lottery with the packet's integrity
		// Corrupt the packet on random bits
		while (rand.nextDouble() < m_error) {
			int bitError = (int) (rand.nextDouble() * (double) packet.length);
			packet[bitError] = (byte) (rand.nextDouble() * 256.0d);
		}

		// Put packet in the arrived packets queue and indicate to interface
		m_arrived.addFirst(packet);
		m_in.indicate(Agent.PACKET_AVAILABLE, this);
	}

	public boolean hasFreeIncoming() {
		return m_in == null;
	}

	public boolean hasFreeOutgoing() {
		return m_out == null;
	}

	public Interface getIncoming() {
		return m_in;
	}

	public void setIncoming(Interface iface) {
		if (m_in == null) {
			m_in = iface;
		} else {
			Log.fatal("Incoming interface on link already occupied.");
			dump();
			System.exit(1);
		}
	}

	public Interface getOutgoing() {
		return m_out;
	}

	public void setOutgoing(Interface iface) {
		if (m_out == null) {
			m_out = iface;
		} else {
			System.err.println("SIMULATOR ERROR: Outgoing interface on link "
					+ "already occupied.");
			dump();
			System.exit(1);
		}
	}

	/**
	 * Set the status of this link, either 'up' or 'down'. Use the integer
	 * constants provided in jns.util.Status.
	 * 
	 * @param status
	 *            the new status of the link
	 */
	public void setStatus(int status) {

		if (status == Status.UP && m_status == Status.DOWN) {
			// Status change to 'up' from 'down'
		} else if (status == Status.DOWN && m_status == Status.UP) {
			// Status change to 'down' from 'up'
			// Drop all packets
			m_packets.clear();
		}

		m_status = status;
	}

	/**
	 * Return the status of this link, either 'up' or 'down'. Returns one of the
	 * constans provided in jns.util.Status.
	 * 
	 * @return Status.UP or Status.DOWN.
	 */
	public int getStatus() {
		return m_status;
	}

	public int getBandwidth() {
		return m_bandwidth;
	}

	public double getDelay() {
		return m_delay;
	}

	public Interface getIncomingInterface() {
		return m_in;
	}

	public Interface getOutgoingInterface() {
		return m_out;
	}

	public boolean canSend() {
		if (m_status == Status.DOWN)
			return false;
		return m_cansend;
	}

	/**
	 * Read a packet that has arrived from this link. Do not call this function
	 * unless you received an indication from the link, otherwise you might get
	 * a null returned.
	 */
	public byte[] read(int unique_id) {
		if (m_status == Status.DOWN)
			return null;
		if (m_arrived.size() > 0) {
			byte[] packet = m_arrived.pollLast();
			return packet;
		} else
			return null;
	}

	public void send(byte[] packet) {
		if (m_status == Status.DOWN)
			return;
		// Put packet in our list and prevent everyone else from sending
		m_packets.addFirst(packet);
		m_cansend = false;

		// There are two things we need to do. First, schedule a command that
		// will make the link clear to send after all the bits are on it

		double transmittime = (double) (packet.length) / (double) m_bandwidth;

		class LinkSendDelayCommand extends Command {
			Link m_link;

			LinkSendDelayCommand(Link l, double time) {
				super("LinkSendDelay", time);
				m_link = l;
			}

			public void execute() {
				// Set send variable to true and indicate to interface that
				// sending is possible
				m_cansend = true;
				m_out.indicate(Agent.READY_TO_SEND, m_link);
			}
		}

		Simulator.getInstance().schedule(
				new LinkSendDelayCommand(this, Simulator.getInstance()
						.getTime() + transmittime));

		// Second, schedule a command for the packet arrival.

		double totaltime = m_delay + transmittime;
		Simulator.getInstance().schedule(
				new ElementUpdateCommand(this, Simulator.getInstance()
						.getTime() + totaltime));
	}

	public static final char debugFlag = 'L';
}
