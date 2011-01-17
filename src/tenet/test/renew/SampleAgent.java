package tenet.test.renew;

import java.io.PrintStream;
import tenet.elem.Agent;
import tenet.elem.ConnectionOrientedAgent;
import tenet.elem.ip.IPAddr;

public class SampleAgent implements ConnectionOrientedAgent {
	ConnectionOrientedAgent low;
	IPAddr srcip;
	IPAddr dstip;
	int port;
	boolean connection_established = false;

	public SampleAgent(int unique_id) {
	}

	public void attach(Agent higher_level, int unique_id) {
	}

	public void attach(Agent lower_level) {
		this.low = ((ConnectionOrientedAgent) lower_level);
	}

	static int counter = 0;
	public void indicate(int status, Object indicator) {
		if ((status & 0x10) != 0) {
			this.connection_established = true;
		}
		if ((status & 0x1) != 0) {
			byte[] data = this.low.read(0);
			System.out.println("received data#" + (++counter) + "["
					+ data.length + "] "
						+ new String(data));
		}
	}

	public boolean canSend(IPAddr destination, int length) {
		return (this.connection_established) && (this.low.isConnected())
				&& (this.low.canSend(destination, length));
	}

	public void connect(IPAddr source, IPAddr destination, int destination_port) {
		if (isConnected())
			return;
		this.low.connect(source, destination, destination_port);
		this.srcip = source;
		this.dstip = destination;
		this.port = destination_port;
	}

	public void disconnect() {
		if (isConnected())
			this.low.disconnect();
		this.connection_established = false;
	}

	public void listen(IPAddr local_ip) {
		if (!isConnected())
			this.low.listen(local_ip);
	}

	public void send(int length, byte[] data, int unique_id) {
		if (canSend(this.dstip, length))
			this.low.send(length, data, 0);
	}

	public byte[] read(int unique_id) {
		return null;
	}

	public boolean isConnected() {
		return (this.connection_established) && (this.low.isConnected());
	}
}

/*
 * Location: D:\My Documents\eclipse\nachos-tenet\tester-0.2.jar Qualified Name:
 * tenet.test.renew.SampleAgent JD-Core Version: 0.6.0
 */