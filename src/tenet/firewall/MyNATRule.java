package tenet.firewall;

import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Packet;

/**
 * No surprise on this struct. NAT Rule here only tells which inside local IP(s)
 * will be sent as some outside IP.
 * 
 * @author Meilun Sheng
 * 
 */
public class MyNATRule extends MyRule {

	public MyNATRule(IPAddr srcIP, IPAddr srcSubnetMask, IPAddr dstIP,
			IPAddr dstSubnetMask, Integer protocol) {
		super(srcIP, srcSubnetMask, dstIP, dstSubnetMask, protocol);
	}

	@Override
	public boolean match(IPPacket packet) {
		return (srcIP.toInt() & srcSubnetMask.toInt()) == (packet.getSourceIP()
				.toInt() & srcSubnetMask.toInt())
				&& matchProtocol(protocol,
						packet.getHeader(IPv4Packet.PROTOCOL));
	}

	@Override
	public IPPacket[] onPacket(IPPacket packet) {
		IPPacket p = new IPv4Packet((IPv4Packet) packet);
		p.setSourceIP(dstIP);
		return new IPPacket[] { p };
	}

}
