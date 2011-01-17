// TODO clone queue at queue attach

package tenet.elem.phys;

import tenet.core.Log;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPHandler;

public class DuplexInterface extends Interface {

	public DuplexInterface() {
		this(null);
	}

	private Interface m_out, m_in;

	public DuplexInterface(IPAddr addr) {
		super(addr);

		m_out = new SimplexInterface(Interface.SENDER, addr);
		m_in = new SimplexInterface(Interface.RECEIVER, addr);
	}

	public DuplexInterface(IPAddr addr, int bandwidth) {
		super(addr, bandwidth);

		m_out = new SimplexInterface(Interface.SENDER, addr, bandwidth);
		m_in = new SimplexInterface(Interface.RECEIVER, addr, bandwidth);
	}

	public void dump() {
		System.out.println("DuplexInterface: " + m_addr);
		m_in.dump();
		m_out.dump();
	}

	public void update() {

	}

	public void setIPHandler(IPHandler handler) {
		super.setIPHandler(handler);
		m_in.setIPHandler(handler);
		m_out.setIPHandler(handler);
	}

	public int getType() {
		return Interface.SENDER | Interface.RECEIVER;
	}

	public Node getNode() {
		return m_in.getNode();
	}

	public void attach(Node node) {
		m_in.attach(node);
		m_out.attach(node);
	}

	public void attach(Link link, boolean inheritBandwidth) {
		if (!(link instanceof DuplexLink)) {
			Log.fatal("You can only attach a DuplexLink to a DuplexInterface!");
			System.exit(1);
		}

		DuplexLink duplexlink = (DuplexLink) link;

		SimplexLink link1 = duplexlink.getSimplexLink1();
		SimplexLink link2 = duplexlink.getSimplexLink2();

		// Check which way round to attach. Note that the part in 'else' might
		// fail and produce an error if someone has played tricks

		if (link1.hasFreeIncoming() && link2.hasFreeOutgoing()) {
			m_in.attach(link1, inheritBandwidth);
			m_out.attach(link2, inheritBandwidth);
		} else {
			m_out.attach(link1, inheritBandwidth);
			m_in.attach(link2, inheritBandwidth);
		}

	}

	public boolean canSend(IPAddr destination, int length) {
		return m_out.canSend(destination, length);
	}

	public void send(byte[] packet) {
		m_out.send(packet);
	}

	public byte[] read(int unique_id) {
		return m_in.read(unique_id);
	}

	public void indicate(int status, Object indicator) {
		m_out.indicate(status, indicator);
		m_in.indicate(status, indicator);
	}

	public void attach(Queue<byte[]> queue) {

	}

	public void setStatus(int status) {
		m_in.setStatus(status);
		m_out.setStatus(status);
	}
}
