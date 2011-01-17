package tenet.droute;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Packet;

public class FragmentCache {

	HashMap<IPAddr, HashMap<Integer, IPPacketFragmentList>> fragCache = new HashMap<IPAddr, HashMap<Integer, IPPacketFragmentList>>();

	LinkedList<IPPacket> done = new LinkedList<IPPacket>();

	static final int EXPIRE_TIME = 20;
	double last_clean = 0;
	public void put(IPPacket packet) {
		if (last_clean + EXPIRE_TIME < MyLib.currentTime()) {
			clean(MyLib.currentTime() - EXPIRE_TIME);
			last_clean = MyLib.currentTime();
		}
		if (packet.getHeader(IPv4Packet.FLAG_MORE_FRAGMENTS) == 0
				&& packet.getHeader(IPv4Packet.FRAGMENT_OFFSET) == 0) {
			done.add(packet);
			return;
		}
		HashMap<Integer, IPPacketFragmentList> map = fragCache.get(packet
				.getSourceIP());
		if (map == null)
			map = new HashMap<Integer, IPPacketFragmentList>();

		int id = packet.getHeader(IPv4Packet.IDENTIFICATION);
		IPPacketFragmentList list = map.get(id);
		if (list == null)
			list = new IPPacketFragmentList(id);
		list.put(packet);
		if (list.ready()) {
			done.add(list.extract());
			map.remove(id);
		}
		else {
			map.put(id, list);
		}
		fragCache.put(packet.getSourceIP(), map);
	}

	public IPPacket get() {
		if (done.isEmpty())
			return null;
		else
			return done.removeFirst();
	}

	public void clean(double time) {
		for (IPAddr addr : fragCache.keySet()) {
			HashMap<Integer, IPPacketFragmentList> map = fragCache.get(addr);
			for (Iterator<IPPacketFragmentList> iter = map.values().iterator(); iter
					.hasNext();) {
				IPPacketFragmentList list = iter.next();
				list.clean(time);
				if (list.isEmpty())
					iter.remove();
			}
		}
	}
}
