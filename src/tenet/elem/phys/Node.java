package tenet.elem.phys;

import tenet.elem.Agent;
import tenet.elem.Element;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPHandler;

import java.util.Enumeration;

/**
 * Node implements a generic network node, i.e. your favourite computer or
 * CISCO's favourite product, a router. This class is not very interesting or
 * important, it merely serves to contain everything (and of course in order not
 * to alienate users of this system).
 */
public class Node extends Element {

	protected IPHandler m_iphandler;
	protected String m_name;
	private int m_number; // Node number (see m_counter)

	// Count the number of nodes (used for NAM/Javis output because they don't
	// understand IP adresses.. so we use numbers).
	private static int m_counter = 0;

	/**
	 * Default constructor, sets the name of this node to "Node" and creates a
	 * new object to deal with IP packets.
	 */
	public Node() {
		m_name = "Node";
		m_iphandler = new IPHandler();
		m_number = m_counter++;
	}

	/**
	 * Constructor to initialise the node, create a handler for IP packets and
	 * set the name of the node.
	 * 
	 * @param name
	 *            a descriptive name of the node
	 */
	public Node(String name) {
		m_name = name;
		m_iphandler = new IPHandler();
		m_number = m_counter++;
	}

	/**
	 * Attach a new interface to this node. An infinite (depending on memory
	 * availability) number of interfaces may be added, they will be put into a
	 * Vector.
	 * 
	 * @param iface
	 *            the interface to attach
	 */
	public void attach(Interface iface) {
		iface.attach(this);
		m_iphandler.attach(iface);
	}

	/**
	 * Attach a new agent to this node's IP handling routines. This is a bit
	 * like installing new protocol software. Note that only protocols that can
	 * run on top of IP can be attached like this (e.g. TCP, SimpleGoBackN,
	 * etc.).<br>
	 * If you want to attach a protocol that runs on top of TCP then:
	 * <ul>
	 * <li>Create a TCP object (after writing one)
	 * <li>Attach your protocol to that
	 * <li>Then attach the TCP object using this function
	 * </ul>
	 * 
	 * @param agent
	 *            the new agent to attach
	 * @param unique_id
	 *            an identifier that will uniquely identify this agent. Please
	 *            use the constants in jns.util.Protocols!
	 */
	public void attach(Agent agent, int unique_id) {
		m_iphandler.attach(agent, unique_id);
	}

	/**
	 * Dump the debugging information about this node.
	 */
	public void dump() {
		System.out.println("Node \"" + m_name + "\"");
		m_iphandler.dump();
	}

	/**
	 * Add an entry to the routing table in this node. JNS uses subnet routing
	 * so all the parameters are specific in standard IP format.
	 * 
	 * @param dest
	 *            the destination subnet address (or host address, if the
	 *            netmask is set to 255.255.255.255)
	 * @param netmask
	 *            the subnet mask of the destination subnet
	 * @param iface
	 *            the interface to give packets destined for this subnet to
	 */
	public void addRoute(IPAddr dest, IPAddr netmask, Interface iface,
			Integer metric) {
		m_iphandler.addRoute(dest, netmask, iface, metric);
	}

	/**
	 * Delete *ALL* route which can match this destination ip.
	 * 
	 * @param dest
	 *            the destination subnet address (or host address, if the
	 *            netmask is set to 255.255.255.255)
	 */
	public void deleteRoute(IPAddr dest) {
		m_iphandler.deleteRoute(dest);
	}

	/**
	 * Add a default routing entry to the routing table of this interface. The
	 * default route will be used if no other routing entry can be matched.
	 * <b>NOTE:</b> You are strongly discouraged to use default routing entries
	 * with routers as you can easily create loops like that. If there are two
	 * routers, R1 and R2 and they both share a link and someone sends a packet
	 * which both of them can't route, they will start playing ping pong using
	 * the default route.
	 * 
	 * @param iface
	 *            the interface to give packets to by default.
	 */
	public void addDefaultRoute(Interface iface) {
		m_iphandler.addDefaultRoute(iface);
	}

	/**
	 * Return this node's node number.
	 */
	public int getNumber() {
		return m_number;
	}

	/**
	 * Return true if an IP address is associated with this Node. A node can
	 * have as many interfaces as you like and each of them can have their own
	 * special IP address.<br>
	 * This function will return true if one of the interfaces in the node has
	 * the given IP address.
	 * 
	 * @param addr
	 *            the IP address to look for
	 * @return true if an interface in this node has the given IP address
	 */
	public boolean hasIPAddress(IPAddr addr) {
		for (Enumeration<Interface> e = m_iphandler.enumerateInterfaces(); e
				.hasMoreElements();) {
			Interface curiface = e.nextElement();
			if (curiface.getIPAddr().equals(addr))
				return true;
		}

		return false;
	}

	public void update() {

	}

	public IPHandler getIPHandler() {
		return m_iphandler;
	}

	public final void setStatus(int status) {
		if (status != m_status)
			m_iphandler.setStatus(status);
		m_status = status;
	}

	public final void reset() {
		m_iphandler.reset();
	}
}
