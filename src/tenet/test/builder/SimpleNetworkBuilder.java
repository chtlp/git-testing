package tenet.test.builder;

import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPv4Addr;

/**
 * an example showing how to use the network builder
 * 
 * @author TLP
 * 
 */
public class SimpleNetworkBuilder extends NetworkBuilder {
	public void build() {
		Node[] nodes = new Node[10];
		IPAddr gate = IPv4Addr.newInstance("192.168.1.0");
		IPAddr subnet = IPv4Addr.newInstance("255.255.255.0");

		for (int i = 0; i < nodes.length; ++i) {
			nodes[i] = addNode(gate, subnet, NodeType.Router, true);
		}

		for (int i = 0; i + 1 < nodes.length; ++i) {
			addLink(nodes[i], nodes[i + 1]);
		}

		int p0 = nodes[0].addTCPAgent();
		int p9 = nodes[9].addTCPAgent();

		listen(nodes[0], p0, 100);
		connect(nodes[9], p9, nodes[0], p0, 100);

		send(nodes[0], p0, "HELLO", 120);

		terminate(3000);

	}

	public static void main(String[] argv) {
		SimpleNetworkBuilder b = new SimpleNetworkBuilder();
		b.build();
		b.export(System.out);
	}
}
