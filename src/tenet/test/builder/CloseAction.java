package tenet.test.builder;

/**
 * represents the action of closing an TCP connection
 * 
 * @author Tang Linpeng
 * 
 */
public class CloseAction extends Action {
	Node node;
	int port;
	double time;

	public CloseAction(Node node, int port, double time) {
		this.node = node;
		this.port = port;
		this.time = time;
	}

	public String toString() {
		return String.format("close %1$s STCP %2$s %3$s ",
				node.addr.toString(),
				port, time);
	}
}
