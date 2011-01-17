package tenet.firewall;

import tenet.elem.Packet;

public class PacketFilter extends AbstractPacketWatcher {

	@Override
	protected Packet[] defaultPackets(Packet packet) {
		return new Packet[] { packet };
	}

}
