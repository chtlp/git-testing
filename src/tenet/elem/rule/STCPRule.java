package tenet.elem.rule;

import tenet.constant.Protocols;
import tenet.elem.ip.IPAddr;

public class STCPRule extends Rule {
	public static final Integer ANY_PORT = -1;
	public static final Integer ACT_ALLOW = 0;
	public static final Integer ACT_DENY = 1;
	protected Integer srcPort;
	protected Integer dstPort;
	protected Integer flag;
	protected Integer action;

	public STCPRule(IPAddr srcIP, IPAddr srcSubnetMask, IPAddr dstIP,
			IPAddr dstSubnetMask, Integer srcPort, Integer dstPort,
			Integer flag, Integer action) {
		super(srcIP, srcSubnetMask, dstIP, dstSubnetMask, Protocols.STCP);
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.flag = flag;
		this.action = action;
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

}
