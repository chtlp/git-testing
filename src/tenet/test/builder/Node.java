package tenet.test.builder;

import java.util.HashSet;
import java.util.LinkedList;

import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPv4Addr;

/**
 * a node in the network
 * 
 * @author TLP
 * 
 */
public class Node implements Comparable<Node> {
	IPAddr addr;
	IPAddr subnetMask;
	NodeType type;
	boolean hasTCP;
	LinkedList<Link> links = new LinkedList<Link>();

	NetworkBuilder buidler;

	public Node(NetworkBuilder builder) {
		this.buidler = builder;
	}

	@Override
	public int compareTo(Node other) {
		return this.addr.toInt() - other.addr.toInt();
	}

	int portCounter = 80;
	HashSet<Integer> allocatedPort = new HashSet<Integer>();

	public int addTCPAgent(int port) {
		if (allocatedPort.contains(port))
			return -1;
		if (port < 0 || port > 65535)
			return -1;
		allocatedPort.add(port);
		return port;
	}

	public int addTCPAgent() {
		while (allocatedPort.contains(portCounter))
			portCounter++;
		return addTCPAgent(portCounter);
	}

	// this function is supposed to be called by NetworkBuilder
	void addLink(NetworkBuilder builder, Link link) {
		IPAddr iface = null;
		if (!links.isEmpty())
			iface = builder.allocate(addr, subnetMask);
		else
			iface = addr;
		if (link.n1 == this)
			link.iface1 = iface;
		else
			link.iface2 = iface;
		links.add(link);
	}

	public String delcareNode() {
		return "node " + type + " " + addr;
	}

	public String declareInterfaces() {
		if (type == NodeType.Host && links.size() > 1)
			throw new NodeTypeError("Host can have at most one link");

		StringBuilder s = new StringBuilder();

		// s.append("iface " + addr + " " + addr + " "
		// + this.buidler.getInterfaceBandwidth() + "\n");

		for (Link l : links) {
			IPAddr iface = null;
			if (l.n1 == this)
				iface = l.iface1;
			else
				iface = l.iface2;
			s.append("iface " + addr + " " + iface + " "
					+ NetworkBuilder.DefaultInterfaceBandWidth + "\n");
		}
		return s.toString();
	}

	public String declareRoutes() {
		StringBuilder s = new StringBuilder();
		IPAddr a0 = IPv4Addr.newInstance(0);
		IPAddr a1 = IPv4Addr.newInstance(~0);
		if (type == NodeType.Host)
			s.append("addroute " + addr + a0 + " " + a0 + " " + addr
					+ NetworkBuilder.DefaultMetric + "\n");
		else {
			// to itself
			s.append("addroute " + addr + " " + addr + " " + a1 + " " + addr
					+ " " + NetworkBuilder.DefaultMetric + "\n");

			// to the hosts
			for (Link l : links) {
				Node other = l.n1 == this ? l.n2 : l.n1;
				if (other.type == NodeType.Router)
					continue;
				IPAddr iface = l.n1 == this ? l.iface1 : l.iface2;
				IPAddr ifaceOther = l.n1 == this ? l.iface2 : l.iface1;
				s.append("addroute " + addr + " " + ifaceOther + " " + a1 + " "
						+ iface + " " + NetworkBuilder.DefaultMetric + "\n");
			}
		}
		return s.toString();
	}

	public String declareAgents() {
		StringBuilder s = new StringBuilder();
		if (allocatedPort.isEmpty())
			return "";
		s.append("tplayer stcp " + addr + " STCP\n");
		for (Integer p : allocatedPort) {
			s.append("newagent hagent " + addr + " STCP " + p + "\n");
		}
		return s.toString();
	}

}
