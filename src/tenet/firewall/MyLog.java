package tenet.firewall;

import tenet.elem.ip.IPPacket;
import tenet.stcp.SimpleTCPPacket;

public class MyLog {
	public static String IPPacket2Str(IPPacket p) {
		StringBuilder b = new StringBuilder();
		if (p.checkCRC() == false)
			b.append("CRC failed IPPacket");
		else {
			b.append("srcIP: " + p.getSourceIP() + ", ");
			b.append("dstIP: " + p.getDestIP() + ", ");

			SimpleTCPPacket tcp = new SimpleTCPPacket();
			boolean bool = tcp.fromBytes(p.getData());
			if (bool == false)
				b.append("unknown content");
			else {
				b.append("srcPort: " + tcp.getSourcePort() + ", ");
				b.append("dstPort: " + tcp.getDestinationPort() + ", ");
			}
		}
		return b.toString();
	}
}
