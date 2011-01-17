package tenet.test.builder;

import java.util.ArrayList;

import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPv4Addr;

public class CircleNetworkBuilder extends NetworkBuilder {

	public void build() {
		ArrayList<ArrayList<Node>> net = new ArrayList<ArrayList<Node>>();
		for (int i = 0; i < 10; ++i) {
			IPAddr gateway = IPv4Addr.newInstance("192.168." + i + ".0");
			IPAddr subnet = IPv4Addr.newInstance("255.255.255.0");
			ArrayList<Node> stub = new ArrayList<Node>();
			for (int j = 0; j < 10; ++j)
				stub.add(addNode(gateway, subnet, NodeType.Router, true));
			for (int j = 1; j < 10; ++j)
				addLink(stub.get(0), stub.get(j));
			net.add(stub);
		}
		for (int i = 0; i < 10; ++i) {
			int j = (i + 1) % 10;
			addLink(net.get(i).get(0), net.get(j).get(0));
		}

		for (int i = 0; i < 10; ++i) {
			Node n1 = net.get(i).get(5);
			Node n2 = net.get((i + 5) % 10).get(5);
			int p1 = n1.addTCPAgent();
			int p2 = n2.addTCPAgent();
			listen(n1, p1, 200);
			connect(n2, p2, n1, p1, 200);
			send(n1, p1, "HELLO" + i, 300);
		}
		terminate(500);
	}

	public static void main(String[] argv) {
		CircleNetworkBuilder b = new CircleNetworkBuilder();
		b.build();
		b.appendHeader("import classSetup\n");
		b.appendHeader("bindtpl stcp stcp_o");
		b.export("cases/circle.tks");
	}

}
