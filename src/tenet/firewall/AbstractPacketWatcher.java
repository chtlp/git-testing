package tenet.firewall;

import tenet.elem.IPacketWatcher;
import tenet.elem.Packet;
import tenet.elem.ip.IPPacket;
import tenet.elem.rule.IRuleExecutable;

public abstract class AbstractPacketWatcher implements IPacketWatcher {

	Firewall ruleHolder;
	int protocol;

	public void attach(IRuleExecutable r, Integer protocol) {
		ruleHolder = (Firewall) r;
		this.protocol = protocol;
	}

	@Override
	public Packet[] onPacket(int direction, Packet packet) {

		for (int i = ruleHolder.size() - 1; i >= 0; --i) {
			MyRule rule = ruleHolder.getRule(i);
			if (rule.match((IPPacket) packet)) {
				return rule.onPacket((IPPacket) packet);
			}
		}

		return defaultPackets(packet);
	}

	protected abstract Packet[] defaultPackets(Packet packet);

}
