package tenet.droute;

import tenet.core.Lib;

public class LinkState {

	public static final int TO_SUBNET = 1;
	public static final int TO_ROUTER = 2;
	public IPSubnet subnet = null;
	public int destRouterID = -1;
	public int metric;
	public double birth;
	
	public LinkState(IPSubnet n, int m) {
		subnet = n;
		metric = m;
		birth = MyLib.currentTime();
	}

	public LinkState(int dst, int m) {
		Lib.assertTrue(dst >= 0);
		destRouterID = dst;
		metric = m;
		birth = MyLib.currentTime();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LinkState) {
			LinkState other = (LinkState) obj;
			if (destRouterID >= 0) {
				return other.destRouterID == destRouterID
						&& other.metric == metric;
			}
			else
				return other.subnet.equals(subnet) && other.metric == metric;
		}
		return false;
	}

	public String toString() {
		if (destRouterID >= 0) {
			return "destRouter=" + destRouterID + " metric=" + metric;
		}
		else {
			return "destSubnet=" + subnet;
		}
	}
}
