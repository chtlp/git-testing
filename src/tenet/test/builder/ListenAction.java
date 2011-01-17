package tenet.test.builder;

/**
 * represents the action of listening on a TCP port
 * 
 * @author TLP
 * 
 */
public class ListenAction extends Action {
	Node node;
	int port;
	double time;

	public ListenAction(Node node, int port, double time) {
		this.node = node;
		this.port = port;
		this.time = time;
	}

	public String toString() {
		return String.format("listen %1$s STCP %2$s %3$s %4$s",
				node.addr.toString(), port, node.addr.toString(), time);
	}
}
