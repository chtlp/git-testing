package tenet.firewall;

import tenet.constant.Protocols;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Packet;
import tenet.elem.rule.Rule;

/**
 * All rules are derived from this class. *: Rule.ANY_PROTOCOL is used to fill
 * the protocol field.\ it means this Rule will be used whatever the protocol of
 * packet is.
 * 
 * @author Meilun Sheng
 * 
 */
public abstract class MyRule extends Rule {

	public MyRule(IPAddr srcIP, IPAddr srcSubnetMask, IPAddr dstIP,
			IPAddr dstSubnetMask, Integer protocol) {
		super(srcIP, srcSubnetMask, dstIP, dstSubnetMask, protocol);
	}

	public boolean match(IPPacket packet) {
		return matchProtocol(protocol, packet.getHeader(IPv4Packet.PROTOCOL))
				&& matchIP(srcIP, srcSubnetMask, packet.getSourceIP())
				&& matchIP(dstIP, dstSubnetMask, packet.getDestIP());
	}

	public boolean matchProtocol(int protocol, int packet_protocol) {
		return protocol == Protocols.ALL || protocol == packet_protocol;
	}

	private boolean matchIP(IPAddr src, IPAddr mask, IPAddr dst) {
		return (src.toInt() & mask.toInt()) == (dst.toInt() & mask.toInt());
	}

	public abstract IPPacket[] onPacket(IPPacket packet);

}
