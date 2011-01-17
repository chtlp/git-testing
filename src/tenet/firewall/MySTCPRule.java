package tenet.firewall;

import tenet.constant.Protocols;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Packet;
import tenet.stcp.SimpleTCPPacket;

public class MySTCPRule extends MyRule {
	public static final Integer ANY_PORT = -1;
	public static final Integer ACT_ALLOW = 0;
	public static final Integer ACT_DENY = 1;

	public static final int CWR = 0x1, ECE = 0x2, URG = 0x4, ACK = 0x8,
			PSH = 0x10, RST = 0x20, SYN = 0x40, FIN = 0x80;

	protected Integer srcPort;
	protected Integer dstPort;
	protected Integer flag;
	protected Integer action;

	public MySTCPRule(IPAddr srcIP, IPAddr srcSubnetMask, IPAddr dstIP,
			IPAddr dstSubnetMask, Integer srcPort, Integer dstPort,
			Integer flag, Integer action) {
		super(srcIP, srcSubnetMask, dstIP, dstSubnetMask, Protocols.STCP);
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.flag = flag;
		this.action = action;
	}

	@Override
	public boolean match(IPPacket packet) {
		return super.match(packet) && tcpMatch(packet);
	}

	private boolean tcpMatch(IPPacket packet) {
		SimpleTCPPacket tcp = new SimpleTCPPacket();
		boolean success = tcp.fromBytes(packet.getData());
		if (!success)
			return false;

		return (srcPort == ANY_PORT || srcPort == tcp.getSourcePort())
				&& (dstPort == ANY_PORT || dstPort == tcp.getDestinationPort())
				&& tcp.getFlag() == flag;
	}

	public Integer getSrcPort() {
		return srcPort;
	}

	public Integer getDstPort() {
		return dstPort;
	}

	public Integer getFlag() {
		return flag;
	}

	public Integer getAction() {
		return action;
	}

	@Override
	public IPPacket[] onPacket(IPPacket packet) {
		IPv4Packet p = new IPv4Packet((IPv4Packet) packet);

		return action == ACT_ALLOW ? new IPPacket[] { p } : new IPPacket[] {};
	}

}
