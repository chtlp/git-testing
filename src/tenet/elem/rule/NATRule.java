package tenet.elem.rule;

import tenet.elem.ip.IPAddr;

/**
 * No surprise on this struct. NAT Rule here only tells which inside local IP(s)
 * will be sent as some outside IP.
 * 
 * @author Meilun Sheng
 * 
 */
public class NATRule extends Rule {

	public NATRule(IPAddr srcIP, IPAddr srcSubnetMask, IPAddr dstIP,
			IPAddr dstSubnetMask, Integer protocol) {
		super(srcIP, srcSubnetMask, dstIP, dstSubnetMask, protocol);
	}

}
