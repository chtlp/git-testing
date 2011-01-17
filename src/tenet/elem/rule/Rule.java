package tenet.elem.rule;

import tenet.elem.ip.IPAddr;

/**
 * All rules are derived from this class. *: Rule.ANY_PROTOCOL is used to fill
 * the protocol field.\ it means this Rule will be used whatever the protocol of
 * packet is.
 * 
 * @author Meilun Sheng
 * 
 */
public abstract class Rule {
	public static final Integer ANY_PROTOCOL = -1;
	protected IPAddr srcIP;
	protected IPAddr srcSubnetMask;
	protected IPAddr dstIP;
	protected IPAddr dstSubnetMask;
	protected Integer protocol;

	public Rule(IPAddr srcIP, IPAddr srcSubnetMask, IPAddr dstIP,
			IPAddr dstSubnetMask, Integer protocol) {
		super();
		this.srcIP = srcIP;
		this.srcSubnetMask = srcSubnetMask;
		this.dstIP = dstIP;
		this.dstSubnetMask = dstSubnetMask;
		this.protocol = protocol;
	}

	public IPAddr getSrcIP() {
		return srcIP;
	}

	public IPAddr getSrcSubnetMask() {
		return srcSubnetMask;
	}

	public IPAddr getDstIP() {
		return dstIP;
	}

	public IPAddr getDstSubnetMask() {
		return dstSubnetMask;
	}

	public Integer getProtocol() {
		return protocol;
	}

}
