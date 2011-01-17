package tenet.firewall;

import tenet.elem.Packet;

public class IIPacketWatcher extends AbstractPacketWatcher {

	@Override
	protected Packet[] defaultPackets(Packet packet) {
		return new Packet[] { packet };
	}

}
