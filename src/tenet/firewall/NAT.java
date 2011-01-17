package tenet.firewall;

import java.util.HashMap;
import java.util.HashSet;

import tenet.elem.Packet;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Addr;
import tenet.elem.ip.IPv4Packet;
import tenet.stcp.SimpleTCPPacket;

public class NAT extends AbstractPacketWatcher {

	static class Pair {
		IPAddr addr;
		int port;

		Pair(IPAddr a, int p) {
			addr = a;
			port = p;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Pair))
				return false;
			Pair other = (Pair) obj;
			return addr.toInt() == other.addr.toInt() && port == other.port;
		}

		@Override
		public int hashCode() {
			return addr.toInt() ^ port;
		}

		@Override
		public String toString() {
			return "<" + addr + ", " + port + ">";
		}

	}

	HashMap<Pair, Pair> inv_mapping = new HashMap<Pair, Pair>();
	HashSet<Integer> portUsed = new HashSet<Integer>();
	int counter = 100;
	int MAXPORT = 65536;

	@Override
	public Packet[] onPacket(int direction, Packet packet) {
		IPPacket p = (IPPacket) packet;
		SimpleTCPPacket tcp = new SimpleTCPPacket();
		boolean b = tcp.fromBytes(p.getData());
		if (b == false)
			return new Packet[] {};

		if (p.getDestIP().toInt() == IPv4Addr.newInstance("192.168.168.1")
				.toInt()) {
			// System.out.println("reply: " + MyLog.IPPacket2Str(p));
			// System.out.println(inv_mapping);
		}

		Pair local = inv_mapping.get(new Pair(p.getDestIP(), tcp
				.getDestinationPort()));

		if (local != null) {
			IPPacket newPacket = new IPv4Packet((IPv4Packet) p);
			tcp.setDestinationPort(local.port);
			newPacket.setDestIP(local.addr);
			newPacket.setData(tcp.toBytes());

			// System.out.println(MyLog.IPPacket2Str(p));
			// System.out.println(MyLog.IPPacket2Str(newPacket));
			// System.out.println("==========================");
			return new Packet[] { newPacket };
		}

		for (int i = ruleHolder.size() - 1; i >= 0; --i) {
			MyRule rule = ruleHolder.getRule(i);
			if (rule.match(p)) {
				IPPacket[] res = rule.onPacket((IPPacket) packet);
				IPPacket newPacket = res[0];

				tcp = new SimpleTCPPacket();
				b = tcp.fromBytes(newPacket.getData());
				if (b == false)
					return new IPPacket[] {};

				Integer outside_port = null;

				int local_port = tcp.getSourcePort();
				Pair pair = new Pair(p.getSourceIP(), local_port);
				for (Pair outside : inv_mapping.keySet()) {
					Pair l = inv_mapping.get(outside);
					if (l.equals(pair)) {
						outside_port = outside.port;
					}
				}

				if (outside_port == null) {
					while (portUsed.contains(counter))
						counter = (counter + 1) % MAXPORT;
					outside_port = counter;
					portUsed.add(counter);
					inv_mapping.put(new Pair(newPacket.getSourceIP(),
							outside_port),
							new Pair(p.getSourceIP(), local_port));
				}

				tcp.setSourcePort(outside_port);
				newPacket.setData(tcp.toBytes());

				// System.out.println(MyLog.IPPacket2Str(p));
				// System.out.println(MyLog.IPPacket2Str(newPacket));
				// System.out.println("==========================");

				return res;
			}
		}

		return defaultPackets(packet);
	}

	@Override
	protected Packet[] defaultPackets(Packet packet) {
		return new Packet[] {};
	}

}
