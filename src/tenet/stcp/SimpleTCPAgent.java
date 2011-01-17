package tenet.stcp;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import tenet.command.Command;
import tenet.core.Config;
import tenet.core.Lib;
import tenet.core.Log;
import tenet.core.Simulator;
import tenet.elem.Agent;
import tenet.elem.ConnectionOrientedAgent;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Packet;

public class SimpleTCPAgent implements ConnectionOrientedAgent {

	public final static int ST_SYN_SENT = 100, ST_LISTENING = 101,
			ST_SYN_RECEIVED = 102, ST_FIN_WAIT_1 = 103, ST_FIN_WAIT_2 = 104,
			ST_WAIT = 105, ST_CLOSE_WAIT = 106, ST_SIM_FIN = 107,
			ST_TIME_WAIT = 108;

	private int state = DISCONNECTED;

	// random seed
	static final Random rand = new Random(100);

	private static final int FAILURE_TIMES = 50;

	int localPort;
	int remotePort;
	int totalSeq;

	int higherLevelID;
	Agent higherLevel;
	SimpleTCP simpleTCP;

	IPAddr source, dest;

	int windowSize = Config.getInteger("stcp.receiverWindow");
	// the advertised window size
	int destWindowSize = Config.getInteger("stcp.receiverWindow");

	SendBuffer sendBuffer = this.new SendBuffer();
	ReceiveBuffer rcvBuffer = this.new ReceiveBuffer();

	RTTEstimation estimation = new RTTEstimation();

	static int counter = 0;
	int number = counter++;
	static SimpleTCPAgent list[] = new SimpleTCPAgent[1000];

	public SimpleTCPAgent(int localPort) {
		this.localPort = localPort;
		list[number] = this;
	}

	@Override
	public void attach(Agent higher_level, int unique_id) {
		if (higherLevel != null) {
			Log.fatal("TCPSimpleAgent can only attach to one higher level agent!");
		}
		higherLevel = higher_level;
		higherLevelID = unique_id;
		higherLevel.attach(this);
	}

	@Override
	public void attach(Agent lower_level) {
		if (!(lower_level instanceof SimpleTCP)) {
			Log.fatal("TCPSimpleAgent must be attached to a SimpleTCP");
		}

		simpleTCP = (SimpleTCP) lower_level;
	}

	@Override
	public void indicate(int status, Object indicator) {
		if (!(indicator instanceof SimpleTCP)) {
			Log.fatal("TCPSimpleAgent must be attached to a SimpleTCP");
		}
		SimpleTCP stcp = (SimpleTCP) indicator;
		Lib.assertTrue(stcp == simpleTCP);

		if (status == Agent.READY_TO_SEND) {
			handleSending();
		}
		else if (status == Agent.PACKET_AVAILABLE) {
			// now the data has been put in the agent port
			IPPacket ipp = new IPv4Packet();
			SimpleTCPPacket p = new SimpleTCPPacket();
			Lib.assertTrue(ipp.fromBytes(simpleTCP.read(localPort)));
			Lib.assertTrue(p.fromBytes(ipp.getData()));
			if (state != ST_LISTENING) {
				// accident control
				if (p.getSourcePort() != remotePort
						|| ipp.getSourceIP().toInt() != dest.toInt()) {
					return;
				}
			}
			switch (state) {
			case ST_LISTENING:
				rcvWhenListening(ipp, p);
				break;
			case ST_SYN_SENT:
				rcvWhenSyn(p);
				break;
			case ST_SYN_RECEIVED:
				rcvWhenSynReceived(ipp, p);
				break;
			case CONNECTION_ESTABLISHED:
			case ST_FIN_WAIT_1:
				handleReceiving(p);
				break;
			case DISCONNECTED:
				Log.info(toString() + " can't receive while disconnected");
				break;
			}
		}
	}

	private void rcvWhenSynReceived(IPPacket ipp, SimpleTCPPacket p) {
		if (p.hasSYN()) {
			// DEBUG
			// rcvWhenListening(ipp, p);
			return;
		}
		// assume established
		Log.debug('G', this + " connection established");
		state = CONNECTION_ESTABLISHED;
		higherLevel.indicate(CONNECTION_ESTABLISHED, this);
		handleReceiving(p);

	}

	private void rcvWhenSyn(SimpleTCPPacket p) {
		if (!p.hasACK() || !p.hasSYN()) {
			Log.info("only handles syn ack now");
			return;
		}
		if (p.getData().length != 1) {
			Log.warn("syn ack out of format");
			return;
		}
		rcvBuffer.head = rcvBuffer.tail = p.getSequence();

		state = CONNECTION_ESTABLISHED;
		Log.debug('G', this + " connection established");
		higherLevel.indicate(CONNECTION_ESTABLISHED, this);
		handleReceiving(p);

	}

	private void rcvWhenListening(IPPacket ipp, SimpleTCPPacket p) {
		if (!p.hasSYN()) {
			Log.info("only handles syn while listening");
			return;
		}
		if (p.getData().length != 1) {
			Log.warn("syn msg out of format");
			return;
		}

		dest = ipp.getSourceIP();
		remotePort = p.getSourcePort();

		totalSeq = rand.nextInt() % 1000 + 1000;
		SimpleTCPPacket synAck = makeNewPacket(new byte[] { 'a' });
		synAck.setACK().setSYN();
		synAck.setACK(p.getSequence() + 1);
		rcvBuffer.head = rcvBuffer.tail = p.getSequence() + 1;

		state = ST_SYN_RECEIVED;
		sendBuffer.waiting.addFirst(synAck);
		handleSending();
	}

	double lastResentTime = 0.0;

	/**
	 * when lower level is ready to send packet, this method will pick packets
	 * from the SendingBuffer and send them.
	 */
	private void handleSending() {
		int w = Math.min(MAX_WINDOW_SIZE, destWindowSize);
		while (!sendBuffer.waiting.isEmpty()) {
			SimpleTCPPacket p = sendBuffer.waiting.getLast();
			if (sendBuffer.isEmpty()
					|| sendBuffer.getHead().getSequence() + w >= p
							.getSequence() + p.getData().length) {
				if (!sendBuffer.isEmpty())
					Log.debug(
							't',
							this
									+ " send space left "
									+ (sendBuffer.getHead().getSequence() + w
											- p.getSequence() - p.getData().length));
				sendBuffer.append(p);
				Log.debug('F', this + " send seq = " + p.getSequence());

				Retransmission retrans = new Retransmission(p);

				lastResentTime = getCurrentTime();
				// retrans.expireInterval = estimation.getTimeOut();
				// retrans.packet = p;

				// p.lastSentTime = Simulator.getInstance().getTime();
				// p.expireInterval = estimation.getTimeOut();
				estimation.recordSend(retrans);

				// assert dest != null;
				if (dest == null) {
					Log.warn(this + " no destination to send the packet");
					return;
				}
				// Lib.assertTrue(p.getData().length <= 4);
				// if (p.getData().length == 4) {
				// Log.debug(this + "send data "
				// + Lib.bytesToInt(p.getData(), 0) + "("
				// + p.getSequence() + ")");
				// if (Lib.bytesToInt(p.getData(), 0) == 25) {
				// Log.debug("Attention");
				// }
				// }
				if (p.hasFIN()) {
					Log.debug('t', toString() + " send fin" + " @ "
							+ Simulator.getInstance().getTime());
					if (sentFINSeq == null)
						sentFINSeq = p.getSequence();
				}
				// p.setACK().setACK(rcvBuffer.head);
				simpleTCP.send(source, dest, p.getLength(), p.toBytes(),
						localPort);
				sendBuffer.waiting.removeLast();

				Simulator.getInstance().schedule(retrans);

			}
			else {
				queryDestWindow();
				break;
			}
		}
	}

	DestWindowQuery query;

	private void queryDestWindow() {
		if (query == null) {
			query = new DestWindowQuery(1);
			query.execute();
		}
		else if (query.queryTimes >= 0) {
			query.queryTimes = 1;
		}
		else {
			query.queryTimes = 1;
			Simulator.getInstance().schedule(query);
		}
	}

	private double getCurrentTime() {
		return Simulator.getInstance().getTime();
	}

	/**
	 * check if all segments are ACKed when in ST_SIM_FIN state
	 */
	private void checkAllSent() {
		if (state == ST_SIM_FIN && sendBuffer.head == sendBuffer.tail) {
			state = DISCONNECTED;
			higherLevel.indicate(DISCONNECTED, this);
		}
	}

	private void terminate() {
		state = DISCONNECTED;
		higherLevel.indicate(DISCONNECTED, this);
		clear();
	}

	/**
	 * when a new SimpleTCPPacket has been received, it will be handled here -
	 * when containing data, it will be ACKed immediately - no SYN is considered
	 */
	private void handleReceiving(SimpleTCPPacket p) {
		// if (getCurrentTime() >= 120) {
		// Log.debug(this + "receiving");
		// }
		ByteState byteState = p.hasFIN() || p.hasSYN() ? ByteState.Specail
				: ByteState.Valid;

		if (p.hasRST()) {
			clear();
			state = ST_LISTENING;

			return;
		}
		if (p.hasACK()) {
			estimation.recordACK(p.getSequence());
			updateACK(p.getACK());
			destWindowSize = p.getWindowSize();
			// DEBUG
			// Lib.assertTrue(destWindowSize <= 20);
			Log.debug('t', "Destination Window Size = " + destWindowSize);
			if (state == ST_FIN_WAIT_1 && sentFINSeq != null
					&& p.getACK() > sentFINSeq) {
				state = ST_FIN_WAIT_2;
			}
			Log.debug('t',
					this + " receive ack with window = " + p.getWindowSize());
		}
		// if (state == ST_FIN_WAIT_1 && p.hasFINACK()) {
		// state = ST_FIN_WAIT_2;
		// }

		if (p.hasFIN()) {
			Log.debug('t', this + " receive fin");
			rcvFin(p);
		}

		Log.debug('t', this + " receive_buffer: p[" + p.getSequence() + ", "
				+ (p.getSequence() + p.getData().length) + ") head: "
				+ rcvBuffer.head);

		if (p.getData().length > 0) {
			if (p.getSequence() + p.getData().length > rcvBuffer.head
					+ windowSize
					&& byteState == ByteState.Valid) { // this should not happen
				Log.warn(this + " receiving data overflow window, drop");

			}
			else {
				rcvBuffer.tail = Math.max(rcvBuffer.tail,
						p.getSequence() + p.getData().length);

				// put the data in the receive buffer
				int seq = p.getSequence();
				byte[] d = p.getData();

				// only handles data in the receiver window
				if (seq >= rcvBuffer.head) {
					for (int i = 0; i < d.length; ++i) {
						int j = (seq + i) % rcvBuffer.buffer.length;
						if (j < 0)
							j += rcvBuffer.buffer.length;
						rcvBuffer.bufferState[j] = byteState;
						rcvBuffer.buffer[j] = d[i];
					}
				}

				if (seq == rcvBuffer.head) {
					boolean hasData = false;
					while (true) {
						int j = rcvBuffer.head % rcvBuffer.buffer.length;
						if (j < 0)
							j += rcvBuffer.buffer.length;
						if (rcvBuffer.bufferState[j] == ByteState.Invalid)
							break;
						if (rcvBuffer.bufferState[j] == ByteState.Valid) {
							rcvBuffer.forwarding[rcvBuffer.top++] = rcvBuffer.buffer[j];
							hasData = true;
						}
						rcvBuffer.bufferState[j] = ByteState.Invalid;
						++rcvBuffer.head;
					}

					if (hasData) {
						higherLevel.indicate(PACKET_AVAILABLE, this);
					}
					Log.debug('t', this + " RcvSeq = " + rcvBuffer.head);
				}
			}

		}

		if (p.getData().length > 0 || !p.hasACK() || p.hasSYN())
			sendACK();
		checkAllSent();

		if (destWindowSize > 0)
			handleSending();
	}

	private void sendACK() {
		SimpleTCPPacket p = makeNewPacket(new byte[] {});
		p.setACK();
		p.setACK(rcvBuffer.head);
		Lib.assertTrue(rcvBuffer.tail >= rcvBuffer.head);
		p.setWindowSize(windowSize - (rcvBuffer.tail - rcvBuffer.head));
		simpleTCP.send(source, dest, p.getLength(), p.toBytes(), localPort);
	}

	/**
	 * update the sender window
	 * 
	 * @param ack
	 *            the sequence number expected by the receiver
	 */
	private void updateACK(int ack) {
		Log.debug('F', this + " update ack " + ack);
		while (!sendBuffer.isEmpty()
				&& sendBuffer.getHead().getSequence() < ack) {
			sendBuffer.head++;
		}
		assert sendBuffer.isEmpty()
				|| sendBuffer.getHead().getSequence() == ack;
	}

	private void rcvFin(SimpleTCPPacket p) {
		// Lib.assertTrue(p.getData().length == 1);
		switch (state) {
		case CONNECTION_ESTABLISHED:
			sendACK();
			SimpleTCPPacket f = makeNewPacket(new byte[] { 'f' });
			f.setFIN();
			// f.setFINACK();

			sendBuffer.waiting.addFirst(f);
			handleSending();
			state = ST_CLOSE_WAIT;
			break;
		case ST_FIN_WAIT_1: // simultaneously sent FIN
			state = ST_SIM_FIN;
			break;
		case ST_FIN_WAIT_2:
			state = ST_TIME_WAIT;
			higherLevel.indicate(DISCONNECTED, this);
			timeWait(0);

			break;
		}
	}

	public static final double TIME_WAIT_LENGTH = 3000.0;
	public static final int TIME_WAIT_RESEND = 3;

	void timeWait(final int i) {
		sendACK();
		if (i < TIME_WAIT_RESEND) {
			Simulator.getInstance().schedule(
					new Command("time wait resend", TIME_WAIT_LENGTH
							/ TIME_WAIT_RESEND + getCurrentTime()) {

						@Override
						public void execute() {
							timeWait(i + 1);
						}
					});
		}

	}

	/**
	 * since TCP always buffer the data, this method will return true
	 */
	@Override
	public boolean canSend(IPAddr destination, int length) {
		return true;
	}

	@Override
	public void connect(IPAddr source, IPAddr destination, int destination_port) {
		Log.debug('t', this + " connect");
		this.source = source;
		this.dest = destination;
		remotePort = destination_port;

		totalSeq = rand.nextInt() % 1000 + 1000;
		SimpleTCPPacket syn = makeNewPacket(new byte[] { 's' });
		syn.setSYN();

		state = ST_SYN_SENT;
		sendBuffer.waiting.addFirst(syn);
		Log.debug('t', this + " send syn(" + syn.getSequence() + ")");
		handleSending();
	}

	// this value will be updated when sending the FIN message
	private Integer sentFINSeq = null;

	@Override
	public void disconnect() {
		Log.debug('t', this + " disconnecting...");
		SimpleTCPPacket fin = makeNewPacket(new byte[] { 'f' });
		fin.setFIN();

		sendBuffer.waiting.addFirst(fin);
		state = ST_FIN_WAIT_1;
		handleSending();
	}

	@Override
	public void listen(IPAddr local_ip) {
		Log.debug('t', this + " listen");
		source = local_ip;
		state = ST_LISTENING;
		clear();
	}

	/**
	 * clear the buffers
	 */
	private void clear() {
		rcvBuffer.top = 0;
		rcvBuffer.head = rcvBuffer.tail = 0;
		sendBuffer.head = sendBuffer.tail = 0;
		totalSeq = -1;
		estimation = new RTTEstimation();
	}

	@Override
	public void send(int length, byte[] data, int unique_id) {
		// Log.debug(this + " sending a number " + Lib.bytesToInt(data, 0));
		switch (state) {
		case CONNECTION_ESTABLISHED:
			// Log.debug(this + "sending data " + Lib.bytesToString(data));
			SimpleTCPPacket p = makeNewPacket(data);
			sendBuffer.waiting.addFirst(p);
			handleSending();
			break;
		default:
			Log.info(this + "Can't send data without a established connection.");
		}
	}

	@Override
	public byte[] read(int unique_id) {
		if (unique_id != higherLevelID) {
			Log.fatal("SimpleTCPAgent read ID unmatch!");
		}

		byte[] ret = Arrays.copyOf(rcvBuffer.forwarding, rcvBuffer.top);
		rcvBuffer.top = 0;
		return ret;
	}

	private SimpleTCPPacket makeNewPacket(byte[] data) {
		SimpleTCPPacket p = new SimpleTCPPacket();
		p.setSourcePort(localPort);
		p.setDestinationPort(remotePort);
		p.setData(data);
		p.setSequence(totalSeq);
		totalSeq += data.length;
		return p;
	}

	@Override
	public String toString() {
		return "TCPAgent(" + number + ")";
	}

	public static final int MAX_WINDOW_SIZE = 10000;
	public static final int MAX_BUFFER_SIZE = 10000;

	private enum ByteState {
		Invalid, Valid, Specail
	}

	class Retransmission extends Command {

		public int sentTimes = 1;
		public double expireInterval = 0.0;
		// the time this action was put to schedule
		public double initTime = 0.0;

		public SimpleTCPPacket packet = null;

		public Retransmission(SimpleTCPPacket packet) {
			super("Retransimission", getCurrentTime() + estimation.getTimeOut());
			expireInterval = estimation.getTimeOut();
			initTime = getCurrentTime();
			this.packet = packet;
		}

		public Retransmission(Retransmission retrans) {
			// super("Retransmission", getCurrentTime() + retrans.expireInterval
			// * (1 << (retrans.sentTimes - 1)));
			super("Retransmission", getCurrentTime() + retrans.expireInterval);

			sentTimes = retrans.sentTimes;
			expireInterval = retrans.expireInterval;
			initTime = getCurrentTime();
			packet = retrans.packet;
		}

		@Override
		public void execute() {
			checkAllSent();
			if (lastResentTime > initTime)
				return;
			if (sendBuffer.head == sendBuffer.tail)
				return;
			SimpleTCPPacket p = sendBuffer.getHead();
			if (state == DISCONNECTED)
				return;
			if (sentTimes++ == FAILURE_TIMES) {
				// Log.debug(
				// 'G',
				// this + " retransmission failure"
				// + new String(p.getData()).substring(0, 10));
				terminate();
			}

			lastResentTime = Simulator.getInstance().getTime();
			// Log.debug(
			// 'G',
			// SimpleTCPAgent.this + " retransmit "
			// + new String(p.getData()).substring(0, 10));
			simpleTCP.send(source, dest, p.getLength(), p.toBytes(), localPort);
			Simulator.getInstance().schedule(new Retransmission(this));
		}

	}

	class ReceiveBuffer {
		byte buffer[] = new byte[MAX_WINDOW_SIZE * 3];
		ByteState bufferState[] = new ByteState[buffer.length];
		{
			for (int i = 0; i < bufferState.length; ++i)
				bufferState[i] = ByteState.Invalid;
		}
		int head, tail;
		byte forwarding[] = new byte[MAX_BUFFER_SIZE];
		int top = 0;
	}

	class SendBuffer {
		SimpleTCPPacket buffer[] = new SimpleTCPPacket[MAX_WINDOW_SIZE
				/ SimpleTCPPacket.HEADER_SIZE * 2];
		int head = 0, tail = 0;
		LinkedList<SimpleTCPPacket> waiting = new LinkedList<SimpleTCPPacket>();

		public SimpleTCPPacket getHead() {
			if (head == tail)
				return null;
			else
				return buffer[head >= 0 ? head % buffer.length : head
						% buffer.length + buffer.length];
		}

		public boolean isEmpty() {
			return head == tail;
		}

		public void append(SimpleTCPPacket p) {
			buffer[tail >= 0 ? tail % buffer.length : tail % buffer.length
					+ buffer.length] = p;
			tail++;
		}
	}

	class RTTEstimation {
		double estimatedRTT = 2;
		double sampleRTT = 5;
		double alpha = 0.125;
		double devRTT = 10;
		double beta = 0.25;

		double lastSendTime;
		int lastSendSeq = -1;

		void recordSend(Retransmission r) {
			if (lastSendSeq < 0) {
				lastSendSeq = r.packet.getSequence();
				lastSendTime = Simulator.getInstance().getTime();
			}
		}

		void recordACK(int seq) {
			if (seq > lastSendSeq) {
				double now = Simulator.getInstance().getTime();
				sampleRTT = now - lastResentTime;
				estimatedRTT = (1 - alpha) * estimatedRTT + alpha * sampleRTT;
				devRTT = (1 - beta) * devRTT + beta
						* Math.abs(sampleRTT - estimatedRTT);
				lastSendSeq = -1;
			}
		}

		double getTimeOut() {
			return estimatedRTT + 4 * devRTT;
		}
	}

	@Override
	public boolean isConnected() {
		return state == CONNECTION_ESTABLISHED;
	}

	class DestWindowQuery extends Command {

		int queryTimes = -1;
		public final int MAX_QUERY_TIMES = 1000;

		public DestWindowQuery() {
			super("dest window query", getCurrentTime() + 10);
		}

		public DestWindowQuery(int t) {
			this();
			queryTimes = t;
		}

		@Override
		public void execute() {
			if (sendBuffer.waiting.isEmpty()) {
				queryTimes = -1;
				return;
			}
			SimpleTCPPacket q = sendBuffer.waiting.getLast();
			// need no further query
			if (sendBuffer.isEmpty()
					|| sendBuffer.getHead().getSequence()
							+ Math.min(MAX_WINDOW_SIZE, destWindowSize) >= q
							.getSequence() + q.getData().length) {
				queryTimes = -1;
				return;
			}

			if (queryTimes == MAX_QUERY_TIMES + 1) {
				queryTimes = -1;
				Log.debug('t', SimpleTCPAgent.this
						+ " query time exceeds limit!");
				return;
			}
			++queryTimes;

			SimpleTCPPacket p = makeNewPacket(new byte[] {});
			simpleTCP.send(source, dest, p.getLength(), p.toBytes(), localPort);
			Log.debug('t', SimpleTCPAgent.this + " dest window query");
			DestWindowQuery dwq = new DestWindowQuery(queryTimes);
			// update the pointer
			query = dwq;
			Simulator.getInstance().schedule(dwq);
		}

	}
}
