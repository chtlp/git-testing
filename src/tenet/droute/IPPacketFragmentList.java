package tenet.droute;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import tenet.core.Lib;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Packet;

public class IPPacketFragmentList {
	LinkedList<IPPacketFragment> list = new LinkedList<IPPacketFragment>();
	int id;
	int payload = -1;
	int currentBytes = 0;
	public IPPacketFragmentList(int id) {
		this.id = id;
	}
	
	void put(IPPacket packet) {
		Lib.assertTrue(packet.getHeader(IPv4Packet.IDENTIFICATION) == id);
		int more_frag = packet.getHeader(IPv4Packet.FLAG_MORE_FRAGMENTS);
		int offset = packet.getHeader(IPv4Packet.FRAGMENT_OFFSET);
		if (payload < 0 && more_frag == 0)
			payload = offset + packet.getData().length;
		// FIXME what is two length is not equal
		for (Iterator<IPPacketFragment> iter = list.iterator(); iter.hasNext();) {
			IPPacketFragment frag = iter.next();
			if (frag.packet.getHeader(IPv4Packet.FRAGMENT_OFFSET) == offset) {
				iter.remove();
				currentBytes -= frag.packet.getData().length;
			}
		}
		IPPacketFragment frag = new IPPacketFragment(packet);
		list.add(frag);
		currentBytes += frag.packet.getData().length;
	}

	boolean ready() {
		// FIXME what is greater than ..
		return payload >= 0 && currentBytes == payload;
	}

	IPPacket extract() {
		if (!ready())
			return null;
		byte[] data = new byte[payload];
		Collections.sort(list);
		int k = 0;
		for (IPPacketFragment f : list) {
			byte[] frag = f.packet.getData();
			for (int i = 0; i < frag.length; ++i, ++k)
				data[k] = frag[i];
		}
		IPPacket res = new IPv4Packet((IPv4Packet) list.getFirst().packet);
		res.setHeader(IPv4Packet.FRAGMENT_OFFSET, 0);
		res.setHeader(IPv4Packet.FLAG_MORE_FRAGMENTS, 0);
		res.setData(data);
		return res;
	}

	void clean(double time) {
		for (Iterator<IPPacketFragment> iter = list.iterator(); iter.hasNext();) {
			if (iter.next().time < time)
				iter.remove();
		}
	}

	boolean isEmpty() {
		return list.isEmpty();
	}
}
