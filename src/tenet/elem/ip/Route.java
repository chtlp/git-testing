package tenet.elem.ip;

import tenet.elem.phys.Interface;

public class Route {

	private IPAddr m_dest;
	private IPAddr m_netmask;
	private Interface m_iface;

	private int m_ttl;
	private Integer m_metric;

	public Route(IPAddr dest, IPAddr netmask, Interface iface, Integer metric) {
		m_dest = dest;
		m_netmask = netmask;
		m_iface = iface;
		m_ttl = 0;
		m_metric = metric;
	}

	public int match(IPAddr target) {
		if ((target.toInt() & m_netmask.toInt()) == (m_dest.toInt() & m_netmask
				.toInt()))
			return Integer.bitCount(m_netmask.toInt());
		else
			return -1;
	}

	public IPAddr getDestination() {
		return m_dest;
	}

	public IPAddr getNetmask() {
		return m_netmask;
	}

	public Interface getInterface() {
		return m_iface;
	}

	public Integer getMetric() {
		return m_metric;
	}

	public void setMetric(Integer metric) {
		m_metric = metric;
	}

	public int getTTL() {
		return m_ttl;
	}

	public void setTTL(int ttl) {
		m_ttl = ttl;
	}

	public void decrementTTL(int amount) {
		m_ttl -= amount;
		if (m_ttl < 0)
			m_ttl = 0;
	}

	public boolean hasTimedOut() {
		return (m_ttl == 0);
	}
}
