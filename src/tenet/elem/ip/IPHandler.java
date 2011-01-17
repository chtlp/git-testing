package tenet.elem.ip;

import java.util.LinkedList;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import tenet.command.ElementUpdateCommand;
import tenet.constant.Status;
import tenet.core.Config;
import tenet.core.Log;
import tenet.core.Simulator;
import tenet.elem.Agent;
import tenet.elem.ConnectionLessAgent;
import tenet.elem.Element;
import tenet.elem.Packet;
import tenet.elem.phys.Interface;

public class IPHandler extends Element implements ConnectionLessAgent {

	// The interfaces attached to this
	protected Vector<Interface> m_interfaces;

	// Packets waiting to be sent
	protected LinkedList<IPPacket> m_packets_send;

	// Received packets to be processed
	protected LinkedList<IPPacket> m_packets_recv;

	// Route table
	protected RoutingTable m_route;

	// Unique packet id
	protected int m_packetid;

	// Higher level protocols, stores instances of 'HigherAgent'
	protected Hashtable<Integer, HigherAgent> m_protocols;

	public IPHandler() {
		m_interfaces = new Vector<Interface>();

		m_packets_send = new LinkedList<IPPacket>();
		m_packets_recv = new LinkedList<IPPacket>();

		m_route = new RoutingTable();

		// Get a new unique packet id start from the current time in
		// milliseconds
		// and a random number between 0-10000.

		// m_packetid=(int)((new
		// Date()).getTime())+(int)(Math.random()*10000.0);
		m_packetid = 0;

		m_protocols = new Hashtable<Integer, HigherAgent>();

		m_status = Status.UP;
	}

	public final void attach(Interface iface) {
		// If the appropriate flag is set, make this iface the default route
		if (Config.getBoolean("system.FirstIfaceIsDefaultRoute")
				&& m_interfaces.isEmpty())
			m_route.addDefaultRoute(iface);

		// Make this handler that interfaces handler
		iface.setIPHandler(this);

		// Add to interface list
		m_interfaces.addElement(iface);
	}

	public final void attach(Agent higher_level, int unique_id) {
		// Create a new receive queue, etc. for this agent
		HigherAgent newagent = new HigherAgent(higher_level);
		m_protocols.put(new Integer(unique_id), newagent);

		// Reverse attach ourselves to this agent (allow it to make a reference)
		higher_level.attach(this);
	}

	public final void attach(Agent lower_level) {
		Log.fatal("You may not attach a lower level agent to an IP "
				+ "handler (because there is no such thing!)");
	}

	public void dump() {
		System.out.println("IP Handler");

		Enumeration<Interface> e = m_interfaces.elements();
		while (e.hasMoreElements())
			e.nextElement().dump();
	}

	public final IPAddr getAddress() {
		if (m_interfaces.size() > 0) {
			Interface iface = (Interface) m_interfaces.firstElement();
			return iface.getIPAddr();
		} else
			return null;
	}

	/**
	 * The update function will process the send queue and the receive queue of
	 * the IPHandler. Everything in the send queue will be given to an interface
	 * to send, if possible. Things in the receive queue will be forwarded to a
	 * higher level protocol or if this is a router, sent on to the next hop.
	 */
	public void update() {
		Log.debug(debugFlag, "Updating IP Handler");

		// Process packets waiting to be sent
		while (m_packets_send.size() > 0) {

			// Remove a packet from the queue
			IPPacket curpacket = (IPPacket) m_packets_send.pollLast();

			// Find a route for the packet
			Interface target = m_route.getRoute(curpacket.getDestIP());

			if (target == null)
				Log.warn("No route to address " + curpacket.getDestIP()
						+ " from " + curpacket.getSourceIP());
			else {

				// See if the interface is free. If not, all this was useless..
				// we put the packet back to the front of the queue
				// TODO: This is not optimal, it prevents packets that go on
				// other
				// links after this one from being sent.

				if (!target.canSend(curpacket.getDestIP(),
						curpacket.getLength())) {
					m_packets_send.addFirst(curpacket);
					break;
				}

				// Check if we have to fragment...
				if (target.getMTU() >= curpacket.getLength())
					target.send(curpacket.toBytes());
				else
					Log.warn("IPHandler cannot handle packets larger than current MTU "
							+ target.getMTU());
			}
		}

		// Received packets waiting to be sent on or given to higher level
		// protocols
		while (m_packets_recv.size() > 0) {
			IPPacket curpacket = m_packets_recv.pollLast();

			// Check the packet's integrity

			if (!curpacket.checkCRC()) {
				Log.warn("Packet CRC checking failed, drop");
				continue;
			}

			// Check if the packet's destination IP address equals on of our
			// interfaces addresses..

			boolean is_final_dest = false;

			Enumeration<Interface> e = m_interfaces.elements();
			while (e.hasMoreElements()) {
				Interface curiface = e.nextElement();

				if (curiface.getIPAddr().equals(curpacket.getDestIP())) {
					Log.debug(debugFlag, "Packet at final dest");
					is_final_dest = true;
					break;
				}
			}

			// If this is not a final destination, send it on
			if (!is_final_dest) {
				Log.debug(debugFlag,
						"Sending on packet from " + curpacket.getSourceIP()
								+ " to " + curpacket.getDestIP());

				m_packets_send.addFirst(curpacket);

				Simulator
						.getInstance()
						.schedule(
								new ElementUpdateCommand(
										this,
										Simulator.getInstance().getTime()
												+ Config.getDouble("system.DelayIPToIfaceQueue")));
			} else {
				// Pass on to higher level protocol
				HigherAgent destagent = (HigherAgent) m_protocols
						.get(new Integer(curpacket
								.getHeader(IPv4Packet.PROTOCOL)));
				if (destagent != null) {
					destagent.queue.addFirst(curpacket);
					destagent.agent.indicate(Agent.PACKET_AVAILABLE, this);
				} else {
					Log.warn("No Agent registered with protocol "
							+ curpacket.getHeader(IPv4Packet.PROTOCOL));
				}
			}
		}
	}

	/**
	 * Send an IP packet. This queues the packet into the send queue and
	 * schedules a call to update().
	 */
	public void send(IPAddr source, IPAddr dest, int length, byte[] data,
			int unique_id) {
		IPPacket packet = new IPv4Packet();
		packet.setSourceIP(IPv4Addr.newInstance(source));
		packet.setDestIP(IPv4Addr.newInstance(dest));
		packet.setData(data);
		// A bug. IDENTIFICATION is used for fragmenting. 
		//
		// Fixed by Chenyang Wu
		// packet.setHeader(IPv4Packet.IDENTIFICATION, m_packetid++);

		packet.setHeader(IPv4Packet.PROTOCOL, unique_id);

		// Put packet in the send queue
		m_packets_send.addFirst(packet);

		// Schedule a call to the update function
		Simulator.getInstance().schedule(
				new ElementUpdateCommand(this, Simulator.getInstance()
						.getTime()
						+ Config.getDouble("system.DelayIPToIfaceQueue")));
	}

	public final byte[] read(int unique_id) {
		HigherAgent destagent = (HigherAgent) m_protocols.get(new Integer(
				unique_id));
		if (destagent == null) {
			Log.warn("Higher level agent with unknown id called read()");
			return null;
		}

		IPPacket packet = (IPPacket) destagent.queue.pollLast();
		return packet == null ? null : packet.toBytes();
	}

	public boolean canSend(IPAddr destination, int length) {
		Interface target = m_route.getRoute(destination);
		if (target == null)
			return false;
		return target.canSend(destination, length);
	}

	/**
	 * Indicate to this IP handler that a packet is waiting for collection
	 * 
	 * @param status
	 *            the status that is indicated, as defined in Agent
	 * @param indicator
	 *            the object that is giving is the packet, must be an Interface
	 *            object.
	 * @see Agent
	 */
	public void indicate(int status, Object indicator) {
		if (!(indicator instanceof Interface))
			Log.fatal("IPHandler received an indication from a non-Interface");

		if ((status & Agent.READY_TO_SEND) != 0) {
			Log.debug(debugFlag, "IPHandler got READY_TO_SEND");

			// Schedule a call to the update function
			Simulator.getInstance().schedule(
					new ElementUpdateCommand(this, Simulator.getInstance()
							.getTime()
							+ Config.getDouble("system.DelayIfaceQueueToIP")));

			// Notify all agents above us that we're ready to send.
			for (Enumeration<HigherAgent> e = m_protocols.elements(); e
					.hasMoreElements();) {
				HigherAgent curagent = (HigherAgent) e.nextElement();

				curagent.agent.indicate(Agent.READY_TO_SEND, this);
			}
		} else if ((status & Agent.PACKET_AVAILABLE) != 0) {
			// IPPacket packet = (IPPacket) ((Interface) indicator).read(0);
			IPPacket packet;
			IPv4Packet v4p = new IPv4Packet();
			boolean accepted = v4p.fromBytes(((Interface) indicator).read(0));
			if (accepted) {
				packet = v4p;
				// Stick the packet in the receive queue
				m_packets_recv.addFirst(packet);

				// Schedule a call to the update function
				Simulator
						.getInstance()
						.schedule(
								new ElementUpdateCommand(
										this,
										Simulator.getInstance().getTime()
												+ Config.getDouble("system.DelayIfaceQueueToIP")));
			}
		} else
			Log.fatal("IPHandler received an unhandled indication: " + status);
	}

	public final void addRoute(IPAddr dest, IPAddr netmask, Interface iface,
			Integer metric) {
		m_route.addRoute(dest, netmask, iface, metric);
	}

	public final void deleteRoute(IPAddr dest) {
		m_route.deleteRoute(dest);
	}

	public final void addDefaultRoute(Interface iface) {
		m_route.addDefaultRoute(iface);
	}

	public final RoutingTable getRoutingTable() {
		return m_route;
	}

	public final Enumeration<Interface> enumerateInterfaces() {
		return m_interfaces.elements();
	}

	public final void setStatus(int status) {
		if (status == Status.DOWN && m_status == Status.UP) {
			for (Interface iface : m_interfaces)
				iface.setStatus(Status.DOWN);
			m_packets_recv.clear();
			m_packets_send.clear();
		} else if (status == Status.UP && m_status == Status.DOWN) {
			for (Interface iface : m_interfaces)
				iface.setStatus(Status.UP);
		}
		m_status = status;
	}

	public final void reset() {
		for (Interface iface : m_interfaces)
			iface.setStatus(Status.DOWN);
		m_packets_recv.clear();
		m_packets_send.clear();
		m_route.clear();
		m_packetid = 0;
		m_status = Status.UP;
	}

	public static final char debugFlag = 'i';

	protected static class HigherAgent {
		public LinkedList<Packet> queue;
		public Agent agent;

		public HigherAgent(Agent agent) {
			this.agent = agent;
			queue = new LinkedList<Packet>();
		}
	}
}
