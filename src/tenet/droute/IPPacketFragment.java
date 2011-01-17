package tenet.droute;

import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Packet;

public class IPPacketFragment implements Comparable<IPPacketFragment> {
	IPPacket packet;
	double time;

	public IPPacketFragment(IPPacket p) {
		packet = p;
		time = MyLib.currentTime();
	}

	@Override
	public int compareTo(IPPacketFragment other) {
		return this.packet.getHeader(IPv4Packet.FRAGMENT_OFFSET)
				- other.packet.getHeader(IPv4Packet.FRAGMENT_OFFSET);
	}
}