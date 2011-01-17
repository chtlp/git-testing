package tenet.test.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import tenet.core.Lib;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPv4Addr;

/**
 * the network builder, inherit this class to use it
 * 
 * @author TLP
 * 
 */
public class NetworkBuilder {

	public static double DefaultError = 0.0;
	public static int DefaultMetric = 1024;
	public static int DefaultInterfaceBandWidth = 1024;
	public static double DefaultDelay = 0.0005;

	/**
	 * override this method to set the error rate
	 * 
	 * @return the error rate of a link
	 */
	public double getErrorRate() {
		return DefaultError;
	}

	public int getMetric() {
		return DefaultMetric;
	}

	public int getInterfaceBandwidth() {
		return DefaultInterfaceBandWidth;
	}

	public double getDelay() {
		return DefaultDelay;
	}

	/**
	 * allocate a IP address of one subnet
	 * 
	 * @param gateway
	 *            the gateway of the subnet
	 * @param mask
	 *            the subnet mask
	 * @return a IP address that has not been allocated by this NetworkBuilder
	 */
	public IPAddr allocate(IPAddr gateway, IPAddr mask) {
		int prefix = gateway.toInt() & mask.toInt();
		int m = mask.toInt();

		// do not allocate the first and the last IP address
		for (int k = 1; (k & m) == 0 && (k + 1 & m) == 0; ++k) {
			int a = prefix | k;
			if (allocatedIP.contains(a))
				continue;
			IPAddr res = IPv4Addr.newInstance(a);
			allocatedIP.add(a);
			return res;
		}
		return null;
	}

	public Node addNode(IPAddr gateway, IPAddr mask, NodeType ty,
			boolean supportTCP) {
		Node node = new Node(this);
		node.addr = allocate(gateway, mask);
		node.subnetMask = mask;
		node.type = ty;
		node.hasTCP = supportTCP;
		nodes.add(node);
		return node;
	}

	Node addHost(IPAddr gateway, IPAddr mask) {
		return addNode(gateway, mask, NodeType.Host, true);
	}

	boolean addLink(Node n1, Node n2, int metric, double delay, double error) {
		Lib.assertTrue(nodes.contains(n1) && nodes.contains(n2));
		Link link = new Link(n1, n2, metric, delay, error);
		links.add(link);
		n1.addLink(this, link);
		n2.addLink(this, link);
		return true;
	}

	boolean addLink(Node n1, Node n2) {
		return addLink(n1, n2, getMetric(), getDelay(), getErrorRate());
	}

	public int addTCPAgent(Node node) {
		return node.addTCPAgent();
	}

	LinkedList<Action> actions = new LinkedList<Action>();

	public void listen(Node node, int port, double time) {
		actions.add(new ListenAction(node, port, time));
	}

	public void connect(Node src, int srcPort, Node dst, int dstPort,
			double time) {
		actions.add(new ConnectAction(src, srcPort, dst, dstPort, time));
	}

	public void send(Node src, int port, String msg, double time) {
		actions.add(new SendAction(src, port, msg, time));
	}

	public void close(Node node, int port, double time) {
		actions.add(new CloseAction(node, port, time));
	}

	public void terminate(double time) {
		actions.add(new TerminateAction(time));
	}


	public void export(PrintStream out) {
		out.println("\n#----------------------- HEADER ---------------------------");
		out.println(header);

		out.println("\n#----------- Nodes, Interfaces & Static Routes-------------");
		for (Node n : nodes) {
			out.println(n.delcareNode());
			out.print(n.declareInterfaces());
			out.print(n.declareRoutes());
			out.print(n.declareAgents());
			out.println();
		}

		out.println("\n#------------------------- Links ---------------------------");
		for (Link l : links) {
			out.println(l.toString());
		}

		out.println("\n#----------------------- Actions ----------------------------");
		for (Action a : actions) {
			out.println(a.toString());
		}

	}

	public void export(String filename) {
		try {
			File file = new File(filename);
			PrintStream out = new PrintStream(file);
			export(out);
		} catch (FileNotFoundException error) {
			throw new Error("file" + filename + " not found!");
		}
	}

	StringBuilder header = new StringBuilder();

	public void appendHeader(String h) {
		header.append(h);
	}

	LinkedHashSet<Node> nodes = new LinkedHashSet<Node>();
	LinkedList<Link> links = new LinkedList<Link>();
	HashSet<Integer> allocatedIP = new HashSet<Integer>();
}
