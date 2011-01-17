package tenet.droute;

import static tenet.droute.MyLib.currentTime;
import static tenet.droute.MyLib.schedule;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import tenet.command.Command;
import tenet.command.ElementUpdateCommand;
import tenet.constant.Status;
import tenet.core.Config;
import tenet.core.Lib;
import tenet.core.Log;
import tenet.core.Simulator;
import tenet.elem.Agent;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPHandler;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Addr;
import tenet.elem.ip.IPv4Packet;
import tenet.elem.phys.Interface;

public class DRNode extends tenet.elem.phys.Node {

	public static final int OSPF_PROTOCOL = 89;

	static final double TIME_RATIO = 1;
	public static final double PERIOD_INTERVAL = 1 * TIME_RATIO;
	public static final double HELLO_INTERVAL = 1 * TIME_RATIO;
	// this is only periodical LinkState advertisement, on-demand advertisement
	// will be much quicker
	public static final double LSA_INTERVAL = 50 * TIME_RATIO;
	public static final double RECALC_TIME = 20 * TIME_RATIO;
	public static final double LSA_AGING = 1000 * TIME_RATIO;

	public static final int HELLO_EXPIRE_RATIO = 6;
	public static final int MAX_RESEND_TIMES = 4;
	public static final int MINIMUM_RESENT_INTERVAL = 4;

	public static final int DEFAULT_METRIC = 1024;

	static int DRCount = 10000;

	public final int routerID = DRCount++;

	DynamicRoutingIPHandler DRIPHandler;

	public DRNode() {
		this("DRNode");
		m_iphandler = DRIPHandler = new DynamicRoutingIPHandler();
		schedule(new PeriodicTask(currentTime() + 0.1));
	}

	public DRNode(String name) {
		// Attention: This constructor must exist!
		this.m_name = name;
		m_iphandler = DRIPHandler = new DynamicRoutingIPHandler();
		// important to wait for a second to start running
		schedule(new PeriodicTask(currentTime() + 0.1));

		// if (name.equals("D2"))
		// schedule(new Command("test",
		// Simulator.getInstance().getTime() + 200) {
		// public void execute() {
		// test();
		// }
		// });
		// if (name.equals("D6"))
		// schedule(new Command("test",
		// Simulator.getInstance().getTime() + 300) {
		// public void execute() {
		// test();
		// }
		// });

	}

	private double last_hello = Integer.MIN_VALUE;
	private double last_lsa = Integer.MIN_VALUE;
	private double last_lsa_clean = Integer.MIN_VALUE;
	private double last_calc = Integer.MIN_VALUE;
	private int seq_number = 100;
	boolean needRecalculation = false;

	void handlePeriodicTasks() {
		/*
		 * if one neighbor has not been sending HELLO for a while, it is dead
		 */
		for (Iterator<NeighborEntry> iter = routerNeighbors.values().iterator(); iter
				.hasNext();) {
			NeighborEntry ent = iter.next();
			if (ent.time + HELLO_INTERVAL * HELLO_EXPIRE_RATIO < currentTime()) {
				iter.remove();
				neighborLinkStateChanged = true;
			}
		}
		if (last_lsa_clean + LSA_AGING < currentTime()) {
			DRTable.cleanLinkStates(currentTime() - LSA_AGING);
			last_lsa_clean = currentTime();
		}

		if (currentTime() > last_hello + HELLO_INTERVAL) {
			OSPFPacket ospf = OSPFPacket.createHelloPacket(routerID,
					seq_number++, 0, currentTime());
			distributePacket(ospf);
			last_hello = currentTime();
		}
		if (currentTime() > last_lsa + LSA_INTERVAL || neighborLinkStateChanged) {
			OSPFPacket ospf = OSPFPacket.createLSAUpdatePacket(routerID,
					seq_number, seq_number, getLocalLinks());
			seq_number++;
			distributePacket(ospf);
			last_lsa = currentTime();
		}

		if (neighborLinkStateChanged) {
			DRTable.upateLinkStates(routerID, getLocalLinks());
			neighborLinkStateChanged = false;
		}
		if (last_calc + RECALC_TIME < currentTime() && needRecalculation) {
			DRTable.calcRoutes();
			needRecalculation = false;
			last_calc = currentTime();
		}

		handleSending();

	}

	ArrayList<LinkState> getLocalLinks() {
		// direct links obtained by HELLO
		ArrayList<LinkState> ls = new ArrayList<LinkState>();
		for (Interface r : routerNeighbors.keySet()) {
			NeighborEntry ent = routerNeighbors.get(r);
			LinkState state = new LinkState(ent.id, ent.metric);
			ls.add(state);
		}
		// static links
		ls.addAll(DRTable.getStaticLinkState());
		return ls;
	}

	private void distributePacket(OSPFPacket ospf) {
		for (Interface i : DRIPHandler.getInterfaces()) {
			HashMap<Integer, PacketRecord> list = controlPackets.get(i);
			if (list == null)
				list = new HashMap<Integer, PacketRecord>();
			list.put(ospf.seqNumber, new PacketRecord(ospf));
			controlPackets.put(i, list);
		}
	}

	protected boolean supportRouting() {
		return true;
	}

	void schedulePeriodicTasks() {
		if (!supportRouting())
			return;
		Log.debug('R', "router schedule");
		handlePeriodicTasks();
		schedule(new PeriodicTask(currentTime() + PERIOD_INTERVAL));
	}

	class PeriodicTask extends Command {

		public PeriodicTask(double time) {
			super("dynamic-routing-periodic-tasks", time);
		}

		@Override
		public void execute() {
			schedulePeriodicTasks();
		}

	}

	class NeighborEntry {
		int id;
		int metric;
		Interface m_interface;
		double time;

		NeighborEntry(int r, int m, Interface itf, double t) {
			id = r;
			m_interface = itf;
			metric = m;
			time = t;
		}
	}

	HashMap<Interface, NeighborEntry> routerNeighbors = new HashMap<Interface, NeighborEntry>();

	public Interface getInterface(Integer routerID) {
		if (routerID == null)
			return null;
		NeighborEntry ent = null;
		for (Interface i : routerNeighbors.keySet()) {
			NeighborEntry e = routerNeighbors.get(i);
			if (e.id == routerID && (ent == null || ent.metric > e.metric))
				ent = e;
		}
		return ent.m_interface;
	}

	// HashMap<Integer, Integer> interface_to_ack = new HashMap<Integer,
	// Integer>();

	static int counter = 0;

	void handleReceivedOSPFPacket(IPPacket ipp, Interface srcInterface) {
		counter++;
		Interface iface = DRIPHandler.getInterface(srcInterface.getIPAddr());
		// DEBUG
		Lib.assertTrue(iface != null);
		OSPFPacket ospf = new OSPFPacket(ipp.getData());
		if (ospf.isValid() == false)
			return;

		switch (ospf.type) {
		case OSPFPacket.HELLO_ACK:
			NeighborEntry ent = routerNeighbors.get(iface);
			if (ent != null && ent.id == ospf.srcID) {
				ent.time = currentTime();
				ent.m_interface = iface;
				ent.metric = DEFAULT_METRIC;
			}
			else {
				ent = new NeighborEntry(ospf.srcID, DEFAULT_METRIC, iface,
						currentTime());
				routerNeighbors.put(iface, ent);
				DRTable.upateLinkStates(routerID, getLocalLinks());
				neighborLinkStateChanged = true;
				handlePeriodicTasks();
				handleSending();
			}
			break;
		case OSPFPacket.HELLO:
			OSPFPacket helloACK = OSPFPacket.createHelloACKPacket(routerID, -1,
					DEFAULT_METRIC, ospf.birth);
			DRIPHandler.sendOSPFPacket(helloACK, iface);
			break;
		case OSPFPacket.LSA_UPDATE:
			OSPFPacket lsaACK = OSPFPacket.createLSAACKPacket(routerID, -1,
					ospf.replyNumber);
			DRIPHandler.sendOSPFPacket(lsaACK, srcInterface);
			handleLSAUpdate(ospf, iface);
			handleSending();
			break;
		case OSPFPacket.LSA_ACK:
			HashMap<Integer, PacketRecord> packets = controlPackets.get(iface);
			// remove the acknowledged packets
			for (Iterator<Integer> iter = packets.keySet().iterator(); iter
					.hasNext();) {
				if (MyLib.seqCmp(iter.next(), ospf.ackNumber) <= 0) {
					iter.remove();
				}
			}
		}
	}

	private void handleSending() {
		for (Interface i : DRIPHandler.getInterfaces()) {
			HashMap<Integer, PacketRecord> map = controlPackets.get(i);
			if (map == null)
				continue;
			for (Iterator<PacketRecord> iter = map.values().iterator(); iter
					.hasNext();) {
				PacketRecord rec = iter.next();
				if (rec.times >= MAX_RESEND_TIMES)
					iter.remove();
				else if (rec.times < MAX_RESEND_TIMES
						&& rec.last + MINIMUM_RESENT_INTERVAL < currentTime()) {
					DRIPHandler.sendOSPFPacket(rec.packet, i);
					rec.times++;
					rec.last = currentTime();
				}
			}
		}
	}

	HashMap<Integer, Integer> routerSeq = new HashMap<Integer, Integer>();

	static class PacketRecord {
		OSPFPacket packet;
		int times = 0;
		double last = -10000;

		public PacketRecord(OSPFPacket p) {
			packet = p;
		}
	}

	HashMap<Interface, HashMap<Integer, PacketRecord>> controlPackets = new HashMap<Interface, HashMap<Integer, PacketRecord>>();

	/*
	 * update LSDB, and broadcast the new message
	 */
	private void handleLSAUpdate(OSPFPacket ospf, Interface srcInterface) {
		// only handles the new packets
		Integer oldSeq = routerSeq.get(ospf.srcID);
		if (oldSeq != null && MyLib.seqCmp(oldSeq, ospf.seqNumber) >= 0)
			return;
		// ospf's sequence number is newer
		routerSeq.put(ospf.srcID, ospf.seqNumber);

		DRTable.upateLinkStates(ospf.srcID, ospf.links);
		// update the replyNumber
		ospf.replyNumber = seq_number++;
		for (Interface i : DRIPHandler.getInterfaces()) {
			// flood the message
			if (i == srcInterface)
				continue;
			HashMap<Integer, PacketRecord> map = controlPackets.get(i);
			if (map == null)
				map = new HashMap<Integer, PacketRecord>();
			map.put(ospf.replyNumber, new PacketRecord(ospf));
			controlPackets.put(i, map);
		}
	}

	boolean neighborLinkStateChanged = false;

	DynamicRoutingTable DRTable;

	class DynamicRoutingIPHandler extends IPHandler {

		@Override
		public void send(IPAddr source, IPAddr dest, int length, byte[] data,
				int unique_id) {
			IPPacket packet = new IPv4Packet();
			packet.setSourceIP(IPv4Addr.newInstance(source));
			packet.setDestIP(IPv4Addr.newInstance(dest));
			packet.setData(data);
			packet.setHeader(IPv4Packet.IDENTIFICATION, (m_packetid++) % 65536);
			packet.setHeader(IPv4Packet.FRAGMENT_OFFSET, 0);
			packet.setHeader(IPv4Packet.FLAG_MORE_FRAGMENTS, 0);

			packet.setHeader(IPv4Packet.PROTOCOL, unique_id);

			// Put packet in the send queue
			m_packets_send.addFirst(packet);

			schedule(new ElementUpdateCommand(this, Simulator.getInstance()
					.getTime() + Config.getDouble("system.DelayIPToIfaceQueue")));
		}

		public DynamicRoutingIPHandler() {
			m_route = DRTable = new DynamicRoutingTable(DRNode.this);
			m_packetid = 1000;
		}

		public Interface getInterface(IPAddr ipAddr) {
			for (Interface i : m_interfaces) {
				if (i.getIPAddr().toInt() == ipAddr.toInt())
					return i;
			}
			return null;
		}

		Vector<Interface> getInterfaces() {
			return m_interfaces;
		}

		FragmentCache frags = new FragmentCache();

		public void indicate(int status, Object indicator) {
			if (getStatus(0) == Status.DOWN)
				return;
			if (!(indicator instanceof Interface))
				Log.fatal("IPHandler received an indication from a non-Interface");

			if ((status & Agent.READY_TO_SEND) != 0) {
				Log.debug(debugFlag, "IPHandler got READY_TO_SEND");

				// Schedule a call to the update function
				schedule(new ElementUpdateCommand(this, Simulator.getInstance()
						.getTime()
						+ Config.getDouble("system.DelayIfaceQueueToIP")));

				// Notify all agents above us that we're ready to send.
				for (Enumeration<HigherAgent> e = m_protocols.elements(); e
						.hasMoreElements();) {
					HigherAgent curagent = (HigherAgent) e.nextElement();

					curagent.agent.indicate(Agent.READY_TO_SEND, this);
				}
			}
			else if ((status & Agent.PACKET_AVAILABLE) != 0) {
				// IPPacket packet = (IPPacket) ((Interface) indicator).read(0);
				IPPacket packet;
				IPv4Packet v4p = new IPv4Packet();
				boolean accepted = v4p.fromBytes(((Interface) indicator)
						.read(0)) && v4p.checkCRC();

				if (accepted
						&& v4p.getHeader(IPv4Packet.PROTOCOL) == OSPF_PROTOCOL) {
					frags.put(v4p);
					v4p = (IPv4Packet) frags.get();
					if (v4p != null)
						handleReceivedOSPFPacket(v4p, (Interface) indicator);
				}
				else if (accepted) {
					packet = v4p;
					// put the packet in the receive queue
					m_packets_recv.addFirst(packet);

					// Schedule a call to the update function
					schedule(new ElementUpdateCommand(this, Simulator
							.getInstance().getTime()
							+ Config.getDouble("system.DelayIfaceQueueToIP")));
				}
			}
			else
				Log.fatal("IPHandler received an unhandled indication: "
						+ status);
		}

		@Override
		public void update() {
			Log.debug(debugFlag, "Updating IP Handler");

			// handle OSPF packets
			for (Interface i : m_interfaces) {
				LinkedList<IPPacket> queue = ospfToSend.get(i);
				if (queue == null)
					continue;
				while (!queue.isEmpty()) {
					IPPacket p = queue.getFirst();
					if (i.canSend(null, p.getLength())) {
						i.send(p.toBytes());
						queue.removeFirst();
					}
					else
						break;
				}
			}

			// handle normal IP packets

			// Process packets waiting to be sent
			while (m_packets_send.size() > 0) {

				// Remove a packet from the queue
				IPPacket curpacket = (IPPacket) m_packets_send.pollLast();

				// Find a route for the packet
				Interface target = m_route.getRoute(curpacket.getDestIP());

				if (target == null) {
					if (curpacket.getDestIP().toInt() != 0xFFFFFFFF)
						Log.warn("No route to address " + curpacket.getDestIP()
								+ " from " + curpacket.getSourceIP());
					// DEBUG
					m_route.getRoute(curpacket.getDestIP());
				}
				else {

					// See if the interface is free. If not, all this was
					// useless..
					// we put the packet back to the front of the queue
					// TODO: This is not optimal, it prevents packets that go on
					// other
					// links after this one from being sent.

					if (!target.canSend(curpacket.getDestIP(),
							curpacket.getLength())) {
						m_packets_send.addLast(curpacket);
						break;
					}

					// Check if we have to fragment...
					if (target.getMTU() >= curpacket.getLength()) {
						target.send(curpacket.toBytes());
					}
					else {
						ArrayList<IPPacket> ps = MyLib.fragment(curpacket,
								target.getMTU());
						m_packets_send.addAll(ps);
					}
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

					schedule(new ElementUpdateCommand(this, Simulator
							.getInstance().getTime()
							+ Config.getDouble("system.DelayIPToIfaceQueue")));
				}
				else {
					frags.put(curpacket);
					curpacket = frags.get();
					if (curpacket != null) {
						// Pass on to higher level protocol
						HigherAgent destagent = (HigherAgent) m_protocols
								.get(new Integer(curpacket
										.getHeader(IPv4Packet.PROTOCOL)));
						if (destagent != null) {
							destagent.queue.addFirst(curpacket);
							destagent.agent.indicate(Agent.PACKET_AVAILABLE,
									this);
						}
						else {
							Log.warn("No Agent registered with protocol "
									+ curpacket.getHeader(IPv4Packet.PROTOCOL));
						}
					}
				}
			}
		}

		HashMap<Interface, LinkedList<IPPacket>> ospfToSend = new HashMap<Interface, LinkedList<IPPacket>>();

		/**
		 * send a OSPF control Packet through an interface
		 */
		void sendOSPFPacket(OSPFPacket ospf, Interface dstInterface) {

			Log.debug('r',
					"router#" + routerID + " " + dstInterface.getIPAddr()
							+ " sending\n" + ospf);
			LinkedList<IPPacket> queue = ospfToSend.get(dstInterface);
			if (queue == null)
				queue = new LinkedList<IPPacket>();
			OSPFPacket p = new OSPFPacket(ospf);

			if (p.type == OSPFPacket.HELLO) {
				p.metric = DEFAULT_METRIC;
			}

			IPPacket ipp = new IPv4Packet();
			ipp.setData(p.toBytes());
			ipp.setSourceIP(IPv4Addr.newInstance(0));
			ipp.setDestIP(IPv4Addr.newInstance(0xFFFFFFFF));
			ipp.setHeader(IPv4Packet.PROTOCOL, OSPF_PROTOCOL);
			ipp.setHeader(IPv4Packet.IDENTIFICATION, (m_packetid++) % 65536);
			ipp.setHeader(IPv4Packet.FRAGMENT_OFFSET, 0);
			ipp.setHeader(IPv4Packet.FLAG_MORE_FRAGMENTS, 0);
			ArrayList<IPPacket> ipps = MyLib.fragment(ipp,
					dstInterface.getMTU());
			queue.addAll(ipps);
			ospfToSend.put(dstInterface, queue);

			schedule(new ElementUpdateCommand(this, Simulator.getInstance()
					.getTime() + Config.getDouble("system.DelayIfaceQueueToIP")));
		}

	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Router(" + routerID + "): [");
		for (Interface i : DRIPHandler.getInterfaces()) {
			builder.append(i.getIPAddr() + " ");
		}
		builder.append("]");
		return builder.toString();
	}

	public void test() {

		System.err.println(m_name + " DOWN");
		this.setStatus(tenet.constant.Status.DOWN);
		final String name = m_name;
		schedule(new Command("UP", Simulator.getInstance().getTime() + 100) {
			public void execute() {
				System.err.println(name + " UP");
				setStatus(tenet.constant.Status.UP);
			}
		});
	}
}
