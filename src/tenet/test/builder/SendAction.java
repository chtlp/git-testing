package tenet.test.builder;

/**
 * 
 * @author TLP
 * 
 */
public class SendAction extends Action {
	Node src;
	int port;
	String msg;
	double time;

	public SendAction(Node src, int port, String msg, double time) {
		this.src = src;
		this.port = port;
		this.msg = msg;
		this.time = time;
	}

	public String toString() {
		return String.format("send %1$s STCP %2$s %3$s %4$s",
				src.addr.toString(),
				port, time,
				msg);
	}
}