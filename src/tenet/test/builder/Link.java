package tenet.test.builder;

import tenet.elem.ip.IPAddr;

/**
 * represents a link between two nodes
 * 
 * @author TLP
 * 
 */
public class Link {
	int metric;
	double delay;
	double error;
	Node n1, n2;
	IPAddr iface1, iface2;

	public Link(Node n1, Node n2, int metric, double delay, double error) {
		this.n1 = n1;
		this.n2 = n2;
		this.metric = metric;
		this.delay = delay;
		this.error = error;
		this.iface1 = null;
		this.iface2 = null;
	}

	public String toString() {
		return String.format("link %1$s %2$s %3$s %4$f %5$s",
				iface1.toString(), iface2.toString(), metric, delay, error);
	}
}
