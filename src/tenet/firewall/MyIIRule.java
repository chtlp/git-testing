package tenet.firewall;

import tenet.constant.Protocols;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Packet;
import tenet.stcp.SimpleTCPPacket;

public class MyIIRule extends MyRule {
	public static final Integer ANY_PORT = -1;
	public static final Integer ACT_ALLOW = 0;
	public static final Integer ACT_RST = 1;
	// by default it matches anything

	public static final int CWR = 0x1, ECE = 0x2, URG = 0x4, ACK = 0x8,
			PSH = 0x10, RST = 0x20, SYN = 0x40, FIN = 0x80;

	protected int srcPort;
	protected int dstPort;
	protected int flag;
	protected int action;
	protected String contentPattern = ".*";

	public MyIIRule(IPAddr srcIP, IPAddr srcSubnetMask, IPAddr dstIP,
			IPAddr dstSubnetMask, Integer srcPort, Integer dstPort,
			Integer flag, Integer action, String content) {
		super(srcIP, srcSubnetMask, dstIP, dstSubnetMask, Protocols.STCP);
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.flag = flag;
		this.action = action;
		this.contentPattern = content;
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

	public String getContent() {
		return contentPattern;
	}

	@Override
	public boolean match(IPPacket packet) {
		return super.match(packet) && portMatch(packet) && contentMatch(packet);
	}

	private boolean portMatch(IPPacket packet) {
		SimpleTCPPacket tcp = new SimpleTCPPacket();
		boolean success = tcp.fromBytes(packet.getData());
		if (!success)
			return false;

		return (srcPort == ANY_PORT || srcPort == tcp.getSourcePort())
				&& (dstPort == ANY_PORT || dstPort == tcp.getDestinationPort());
	}

	private boolean contentMatch(IPPacket packet) {
		SimpleTCPPacket p = new SimpleTCPPacket();
		boolean b = p.fromBytes(packet.getData());
		if (!b)
			return false;
		return p.getFlag() == flag
				&& new String(p.getData()).matches(contentPattern);

	}

	@Override
	public IPPacket[] onPacket(IPPacket packet) {
		IPPacket p = new IPv4Packet((IPv4Packet) packet);
		if (action == ACT_ALLOW)
			return new IPPacket[] { p };
		else if (action == ACT_RST) {
			SimpleTCPPacket t = new SimpleTCPPacket();
			boolean s = t.fromBytes(p.getData());
			if (s) {
				t.setFlag(RST);
				p.setData(t.toBytes());
			}
			return new IPPacket[] { p };
		}
		throw new Error("unsupported action type");
	}

}
