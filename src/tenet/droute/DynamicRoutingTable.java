package tenet.droute;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import tenet.core.Lib;
import tenet.core.Log;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPv4Addr;
import tenet.elem.ip.Route;
import tenet.elem.ip.RoutingTable;
import tenet.elem.phys.Interface;

public class DynamicRoutingTable extends RoutingTable {

	// private DRNode node;
	private Route m_default; // Default route
	private Vector<Route> m_routes; // List of 'Route' objects
	DRNode myRouter;

	public DynamicRoutingTable(DRNode router) {
		m_default = null;
		m_routes = new Vector<Route>();
		myRouter = router;
	}

	static class Entry {
		int metric;
		double birth;

		public Entry(int m) {
			metric = m;
			birth = MyLib.currentTime();
		}

		public String toString() {
			return "(v = " + metric + ", t = " + birth + ")";
		}
	}

	static class SubnetEntry {
		IPSubnet subnet;
		int router;
		int metric;
		double birth;

		public SubnetEntry(IPSubnet n, int r, int m) {
			subnet = n;
			router = r;
			metric = m;
			birth = MyLib.currentTime();
		}

		public String toString() {
			return "(router = " + router + " v = " + metric + ", t = " + birth
					+ ")";
		}
	}

	HashMap<Integer, HashMap<Integer, Entry>> links = new HashMap<Integer, HashMap<Integer, Entry>>();
	LinkedList<SubnetEntry> destSubnet = new LinkedList<SubnetEntry>();

	public ArrayList<LinkState> getStaticLinkState() {
		ArrayList<LinkState> routes = new ArrayList<LinkState>();
		for (Route r : m_routes) {
			routes.add(new LinkState(new IPSubnet(r.getDestination().toInt(), r
					.getNetmask().toInt()), r.getMetric()));
		}
		return routes;
	}

	public void upateLinkStates(int router, ArrayList<LinkState> ls) {
		myRouter.needRecalculation = myRouter.needRecalculation
				|| hasDifference(router, ls);

		for (Iterator<SubnetEntry> iter = destSubnet.iterator(); iter.hasNext();) {
			if (iter.next().router == router)
				iter.remove();
		}
		HashMap<Integer, Entry> edges = new HashMap<Integer, Entry>();
		for (LinkState link : ls) {
			if (link.destRouterID < 0) {
				destSubnet
						.add(new SubnetEntry(link.subnet, router, link.metric));
			}
			else {
				Entry ent = edges.get(link.destRouterID);
				if (ent != null && ent.metric < link.metric)
					;
				else
					edges.put(link.destRouterID, new Entry(link.metric));
			}
		}
		links.put(router, edges);

		// calcRoutes();

	}

	private boolean hasDifference(int router, ArrayList<LinkState> ls) {
		int s1 = 0, l1 = 0;
		for (SubnetEntry e : destSubnet) {
			if (e.router == router)
				s1++;
		}
		HashMap<Integer, Entry> edges = links.get(router);
		l1 = edges == null ? 0 : edges.size();

		int s2 = 0, l2 = 0;
		for (LinkState s : ls) {
			if (s.destRouterID >= 0)
				l2++;
			else
				s2++;
		}
		if (l1 != l2 || s1 != s2)
			return true;

		for (LinkState s : ls) {
			if (s.destRouterID >= 0) {
				Entry ent = edges.get(s.destRouterID);
				if (ent == null || ent.metric != s.metric)
					return true;
			}
			else {
				// we don't care about the static links
			}
		}
		return false;
	}

	HashMap<Integer, Integer> distance = new HashMap<Integer, Integer>();
	HashMap<Integer, Integer> prev = new HashMap<Integer, Integer>();

	public void calcRoutes() {
		HashMap<Integer, Integer> oldDist = new HashMap<Integer, Integer>(
				distance);

		for (Integer r : links.keySet()) {
			distance.put(r, Integer.MAX_VALUE);
			prev.put(r, -1);
			HashMap<Integer, Entry> edge = links.get(r);
			for (Integer s : edge.keySet()) {
				distance.put(s, Integer.MAX_VALUE);
				prev.put(s, -1);
			}
		}
		HashSet<Integer> done = new HashSet<Integer>();
		done.add(myRouter.routerID);
		distance.put(myRouter.routerID, 0);
		prev.put(myRouter.routerID, -1);
		HashMap<Integer, Entry> localEdges = links.get(myRouter.routerID);
		if (localEdges == null)
			return;

		for (Integer r : localEdges.keySet()) {
			distance.put(r, localEdges.get(r).metric);
			prev.put(r, myRouter.routerID);
		}

		while (done.size() < distance.size()) {
			int x = -1, d = Integer.MAX_VALUE, tmp;
			for (Integer r : distance.keySet()) {
				if (done.contains(r))
					continue;
				if ((tmp = distance.get(r)) < d) {
					d = tmp;
					x = r;
				}
			}

			if (x == -1)
				break;

			HashMap<Integer, Entry> edges = links.get(x);
			if (edges != null) {
				for (Integer y : edges.keySet()) {
					Entry ent = edges.get(y);
					if (!done.contains(y) && ent != null
							&& distance.get(y) > d + ent.metric) {
						distance.put(y, d + ent.metric);
						prev.put(y, x);
					}
				}
			}
			done.add(x);
		}

		// DEBUG
		boolean updated = false;
		for (Integer r : distance.keySet()) {
			Integer d1 = oldDist.get(r);
			Integer d2 = distance.get(r);
			if (d1 == null && d2 != null || d1 != null && d2 != null && d1 > d2) {
				updated = true;
			}
		}

		if (updated) {
			if (Log.testFlag('R')) {
				Log.debug('R', "RoutingTable[" + myRouter.routerID + "]: "
						+ distance);
				for (SubnetEntry e : destSubnet)
					Log.debug('R', "static route " + e.subnet + " from "
							+ e.router);
			}
		}

	}

	/**
	 * get the next hop of the dynamic route
	 * 
	 * @param addr
	 *            the destination
	 * @return the ID of the next hop
	 */
	public Integer getDynamicRoute(IPAddr addr) {
		int match = -1;
		int metric = Integer.MAX_VALUE;
		int dist = Integer.MAX_VALUE;
		SubnetEntry entry = null;
		for (SubnetEntry ent : destSubnet) {
			Integer d = distance.get(ent.router);
			if (d == null || d == Integer.MAX_VALUE)
				continue;
			int ma = ent.subnet.match(addr);
			if (ma < match)
				continue;
			if (ma == match && ent.metric > metric)
				continue;
			if (ma == match && ent.metric == metric && d > dist)
				continue;
			entry = ent;
			match = ma;
			metric = ent.metric;
			dist = d;
		}
		if (match <= 0)
			return null;
		return nextHop(entry.router);
	}

	private Integer nextHop(Integer r) {
		if (r == null || distance.get(r) == null
				|| distance.get(r) == Integer.MAX_VALUE)
			return null;
		while (true) {
			int t = prev.get(r);
			if (t == -1 || t == myRouter.routerID)
				break;
			r = t;
		}
		Lib.assertTrue(prev.get(r).equals(myRouter.routerID));
		return r;
	}

	public Route getLocalRoute(Interface itf) {
		for (Route r : m_routes) {
			if (r.getInterface() == itf)
				return r;
		}
		return null;
	}

	/*
	 * I have to override all the methods below because I use the private
	 * members
	 */

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
		if (result == null) {
			result = myRouter.getInterface(getDynamicRoute(dest));
		}
		// still no dynamic routes to the destination
		if (result == null && m_default != null)
			result = m_default.getInterface();
		return result;
	}

	public void addRoute(IPAddr dest, IPAddr netmask, Interface iface,
			Integer metric) {
		Route route = new Route(dest, netmask, iface, metric);

		// TODO: Check for duplicate routes and give a warning
		m_routes.addElement(route);

		// recalculate the routes
		upateLinkStates(myRouter.routerID, myRouter.getLocalLinks());
	}

	public void addDefaultRoute(Interface iface) {
		Route route = new Route(IPv4Addr.newInstance(0, 0, 0, 0),
				IPv4Addr.newInstance(255, 255, 255, 255), iface, 200);
		m_default = route;
	}

	public void deleteRoute(IPAddr dest) {
		Enumeration<Route> e = m_routes.elements();
		while (e.hasMoreElements()) {
			Route curroute = e.nextElement();
			if (curroute.match(dest) != -1)
				m_routes.remove(curroute);
		}
	}

	public void clear() {
		m_default = null;
		m_routes.clear();
	}

	public Enumeration<Route> enumerateEntries() {
		return m_routes.elements();
	}

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

	public void cleanLinkStates(double time) {
		for (Iterator<SubnetEntry> iter = destSubnet.iterator(); iter.hasNext();) {
			SubnetEntry ent = iter.next();
			if (ent.birth < time) {
				iter.remove();
				myRouter.needRecalculation = true;
			}
		}

		for (Iterator<HashMap<Integer, Entry>> iter = links.values().iterator(); iter
				.hasNext();) {
			HashMap<Integer, Entry> edges = iter.next();
			for (Iterator<Entry> iter2 = edges.values().iterator(); iter2
					.hasNext();) {
				Entry ent = iter2.next();
				if (ent.birth < time) {
					iter2.remove();
					myRouter.needRecalculation = true;
				}
			}
		}
		// end of clean operation
	}

}
