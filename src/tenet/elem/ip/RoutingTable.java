package tenet.elem.ip;

import java.util.Enumeration;
import java.util.Vector;

import tenet.elem.phys.Interface;

/**
 * RoutingTable implements a generic IP routing table. There are routes to
 * networks associated with interfaces and a default route. Note: The current
 * implementation uses a vector for the routes. A tree structure would be
 * faster, but there are no efficiency concers. Feel free to change it.
 */
public class RoutingTable {

	private Route m_default; // Default route
	private Vector<Route> m_routes; // List of 'Route' objects

	public RoutingTable() {
		m_default = null;
		m_routes = new Vector<Route>();
	}

	public Interface getRoute(IPAddr dest) {
		Enumeration<Route> e = m_routes.elements();
		Interface result = null;
		int score = -1;
		while (e.hasMoreElements()) {
			Route curroute = (Route) e.nextElement();
			if (curroute.match(dest) > score) {
				score = curroute.match(dest);
				result = curroute.getInterface();
			}
		}
		if (result == null && m_default != null)
			result = m_default.getInterface();
		return result;
	}

	/**
	 * Add a route to the routing table.
	 * 
	 * @param dest
	 *            the destination address (network or host)
	 * @param netmask
	 *            the netmask to use when comparing against targets
	 * @param iface
	 *            the interface to send packets to when sending to dest
	 */
	public void addRoute(IPAddr dest, IPAddr netmask, Interface iface,
			Integer metric) {
		Route route = new Route(dest, netmask, iface, metric);

		// TODO: Check for duplicate routes and give a warning
		m_routes.addElement(route);
	}

	/**
	 * Set the default route to use when no other route can be matched. Note
	 * that repeated calls will override the previous default route.
	 * 
	 * @param iface
	 *            the interface to send packets to when they can't be routed
	 */
	public void addDefaultRoute(Interface iface) {
		Route route = new Route(IPv4Addr.newInstance(0, 0, 0, 0),
				IPv4Addr.newInstance(255, 255, 255, 255), iface, 200);
		m_default = route;
	}

	/**
	 * Delete every route to the given destination.
	 */
	public void deleteRoute(IPAddr dest) {
		Enumeration<Route> e = m_routes.elements();
		while (e.hasMoreElements()) {
			Route curroute = e.nextElement();
			if (curroute.match(dest) != -1)
				m_routes.remove(curroute);
		}
	}

	/**
	 * Delete every route.
	 */
	public void clear() {
		m_default = null;
		m_routes.clear();
	}

	public Enumeration<Route> enumerateEntries() {
		return m_routes.elements();
	}

	/**
	 * Delete the default route.
	 */
	public void deleteDefaultRoute() {
		m_default = null;
	}

	public void dump() {
		Enumeration<Route> e = m_routes.elements();
		while (e.hasMoreElements()) {
			Route curroute = e.nextElement();
			System.out.println(curroute.getDestination() + "      "
					+ curroute.getNetmask());
		}
	}

}
