package tenet.test.builder;

/**
 * represents the action of connecting to another TCP port
 * 
 * @author Tang Linpeng
 * 
 */
public class ConnectAction extends Action {
	Node src, dst;
	int srcPort, dstPort;
	double time;

	public ConnectAction(Node src, int srcPort, Node dst, int dstPort,
			double time) {
		this.src = src;
		this.dst = dst;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.time = time;
	}

	public String toString() {
		return String.format("connect %1$s STCP %2$s %1$s %3$s %4$s %5$s",
				src.addr.toString(), srcPort, dst.addr.toString(), dstPort,
				time);
	}
}
