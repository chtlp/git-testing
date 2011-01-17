package tenet.firewall;

import static tenet.droute.MyLib.schedule;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import tenet.command.ElementUpdateCommand;
import tenet.constant.Protocols;
import tenet.core.Config;
import tenet.core.Log;
import tenet.core.Simulator;
import tenet.droute.FragmentCache;
import tenet.droute.MyLib;
import tenet.elem.Agent;
import tenet.elem.IPacketWatcher;
import tenet.elem.IPacketWatcherHolder;
import tenet.elem.Packet;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPHandler;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Addr;
import tenet.elem.ip.IPv4Packet;
import tenet.elem.phys.Interface;
import tenet.elem.phys.Node;
import tenet.elem.rule.IRuleExecutable;
import tenet.elem.rule.Rule;

/*
 * FireWall, supports MTU fragmentation
 */
public class Firewall extends Node implements IPacketWatcherHolder,
		IRuleExecutable {

	ArrayList<MyRule> ruleList = new ArrayList<MyRule>();

	public Firewall() {
		super();
		m_iphandler = new PacketProcessor();
	}

	public Firewall(String name) {
		super(name);
		m_iphandler = new PacketProcessor();
	}

	@Override
	public int addRule(Rule rule) {
		ruleList.add((MyRule) rule);
		return 0;
	}

	@Override
	public int addRule(Rule rule, int index) {
		ruleList.add(index, (MyRule) rule);
		return index;
	}

	@Override
	public boolean deleteRule(Rule rule) {
		return ruleList.remove(rule);
	}

	@Override
	public MyRule getRule(int index) {
		return ruleList.get(index);
	}

	public int size() {
		return ruleList.size();
	}

	Hashtable<Integer, IPacketWatcher> watchers = new Hashtable<Integer, IPacketWatcher>();
	HashSet<Integer> allProtocols = new HashSet<Integer>();
	{
		allProtocols.add(Protocols.STCP);
	}

	@Override
	public void setPacketWatcher(IPacketWatcher watcher, Integer protocol) {
		if (protocol.equals(Protocols.ALL)) {
			for (Integer p : allProtocols) {
				setPacketWatcher(watcher, p);
			}
			return;
		}

		((AbstractPacketWatcher) watcher).attach(this, protocol);
		watchers.put(protocol, watcher);

	}

	class PacketProcessor extends IPHandler {

		public PacketProcessor() {
			m_packetid = 1000;
		}

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

		FragmentCache cache = new FragmentCache();

		@Override
		public void indicate(int status, Object indicator) {
			if (!(indicator instanceof Interface))
				Log.fatal("IPHandler received an indication from a non-Interface");

			if ((status & Agent.READY_TO_SEND) != 0) {
				Log.debug(debugFlag, "IPHandler got READY_TO_SEND");

				schedule(new ElementUpdateCommand(this, Simulator.getInstance()
						.getTime()
						+ Config.getDouble("system.DelayIfaceQueueToIP")));

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
						.read(0));

				if (accepted && v4p.checkCRC()) {
					cache.put(v4p);
				}
				v4p = (IPv4Packet) cache.get();

				if (v4p != null) {
					// Log.debug('G', "" + v4p.getDestIP());
					packet = v4p;
					// Stick the packet in the receive queue
					IPPacket[] packets = processPacket(packet);
					for (IPPacket p : packets)
						m_packets_recv.addFirst(p);

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

					// See if the interface is free. If not, all this was
					// useless..
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
					else {
						ArrayList<IPPacket> packets = MyLib.fragment(curpacket,
								target.getMTU());
						m_packets_send.addAll(packets);
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

					Simulator
							.getInstance()
							.schedule(
									new ElementUpdateCommand(
											this,
											Simulator.getInstance().getTime()
													+ Config.getDouble("system.DelayIPToIfaceQueue")));
				}
				else {

					cache.put(curpacket);
					curpacket = cache.get();
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

		protected IPPacket[] processPacket(IPPacket packet) {
			IPacketWatcher watcher = watchers.get(packet
					.getHeader(IPv4Packet.PROTOCOL));
			if (watcher == null)
				return new IPPacket[] { packet };

			Packet[] newPackets = watcher.onPacket(0, packet);

			IPPacket[] ret = new IPPacket[newPackets.length];
			for (int i = 0; i < newPackets.length; ++i) {
				ret[i] = (IPPacket) newPackets[i];
			}
			return ret;
		}

	}

}
