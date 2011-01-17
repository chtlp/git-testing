package tenet.stcp;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Random;

import tenet.constant.Protocols;
import tenet.core.Log;
import tenet.elem.Agent;
import tenet.elem.ConnectionLessAgent;
import tenet.elem.Packet;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPHandler;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Packet;

public class SimpleTCP implements ConnectionLessAgent {

	// A hashtable containing AgentPort objects, indexed by port
	// numbers as Integer objects
	private Hashtable<Integer, AgentPort> m_ports;

	// The IP service we are using (note that we are using a CL_Agent reference
	// so any connection-less service will do for us!)
	private ConnectionLessAgent m_ip;

	static Double failure_rate;
	static Random random = new Random(100);

	public SimpleTCP() {
		m_ports = new Hashtable<Integer, AgentPort>();

		if (failure_rate == null) {
			// failure_rate = Config.getDouble("system.TCPFailureRate", 0.0);
			failure_rate = 0.0;
		}
	}

	/**
	 * Return a new agent that can be used to open a connection from a specific
	 * source port. This basically takes care of setting up the agent, attaching
	 * it to this class, etc. You can just go ahead and send data afterwards
	 * (after connecting, etc.)
	 * 
	 * @param localPort
	 *            the port at which to open the agent. If you use a duplicate
	 *            port you will get a simulator error displayed.
	 */
	public final SimpleTCPAgent createNewAgent(int localPort) {
		SimpleTCPAgent newAgent = new SimpleTCPAgent(localPort);
		attach(newAgent, localPort);
		return newAgent;
	}

	/**
	 * Attach a higher level agent to this multiplexer. Only SimpleGoBackNAgent
	 * objects may be attached.
	 * 
	 * @param higher_level
	 *            an instance of SimpleGoBackNAgent
	 * @param unique_id
	 *            the port number of the agent.
	 */
	@Override
	public void attach(Agent higher_level, int unique_id) {
		// Check for right agent type
		if (!(higher_level instanceof SimpleTCPAgent))
			Log.fatal("SimpleTCP can only attach SimpleTCPAgent");

		// Check for duplicate port assignments
		if (m_ports.get(unique_id) != null)
			Log.fatal("SimpleTCP can only attach one agent per port");

		higher_level.attach(this);
		m_ports.put(unique_id, new AgentPort(higher_level));
	}

	/**
	 * Attach callback for lower level agents. SimpleGoBackN can only run on top
	 * of IP.
	 * 
	 * @param lower_level
	 *            an instance of IPHandler
	 */
	@Override
	public void attach(Agent lower_level) {
		// Check for correct lower-level agent (IP)
		if (!(lower_level instanceof IPHandler)) {
			Log.fatal("SimpleTCP can only run on top of IP!");
			System.exit(1);
		}

		m_ip = (ConnectionLessAgent) lower_level;
	}

	/**
	 * Indicate an event to SGN.
	 * 
	 * @see tenet.elem.Agent
	 */
	@Override
	public void indicate(int status, Object indicator) {
		if (status == Agent.READY_TO_SEND) {
			handleSending();
			for (Enumeration<AgentPort> e = m_ports.elements(); e
					.hasMoreElements();)
				e.nextElement().agent.indicate(Agent.READY_TO_SEND, this);
		}
		else if (status == Agent.PACKET_AVAILABLE) {
			// now here comes some packets that I must forward to the upper
			// layer

			IPHandler ip = (IPHandler) indicator;
			byte[] data = ip.read(Protocols.STCP);
			IPPacket ipp = new IPv4Packet();
			if (ipp.fromBytes(data) == false) {
				Log.warn("IP Packet corruption");
			}
			SimpleTCPPacket tcp = new SimpleTCPPacket();
			if (tcp.fromBytes(ipp.getData()) == false) {
				Log.debug('t', "TCP packet corruption");
				return;
			}

			if (failure_rate != null && random.nextDouble() < failure_rate) {
				return;
			}
			// if (tcp.getData().length == 4) {
			// Log.debug(this + " receive num: "
			// + Lib.bytesToInt(tcp.getData(), 0));
			// }

			AgentPort listener = m_ports.get(tcp.getDestinationPort());
			if (listener == null) {
				Log.warn("TCP packet sent to port without a listener!");
			}
			else {
				listener.packets.addFirst(ipp);
				listener.agent.indicate(PACKET_AVAILABLE, this);
			}

		}
	}

	/**
	 * This agent will always accept packets from the higher level and buffer
	 * them. So I'll just buffer the packets
	 */
	@Override
	public boolean canSend(IPAddr destination, int length) {
		return true;
	}

	/**
	 * The send function is used by SimpleGoBackNAgent objects to send data (and
	 * noone else, it's internal to this protocol).
	 * 
	 * @param data
	 *            an instance of SimpleGoBackNPacket.
	 */
	@Override
	public void send(IPAddr source, IPAddr dest, int length, byte[] data,
			int unique_id) {
		// Lib.assertTrue(data.length >= SimpleTCPPacket.HEADER_SIZE);
		PendingTask t = new PendingTask();
		t.src = source;
		t.dst = dest;
		t.length = length;
		t.data = data;
		t.id = Protocols.STCP;

		toSend.addFirst(t);
		// assert t.dst != null;
		handleSending();
	}

	private void handleSending() {
		while (!toSend.isEmpty()) {
			PendingTask t = toSend.getLast();
			if (m_ip.canSend(t.dst, t.length)) {
				assert t.dst != null;
				m_ip.send(t.src, t.dst, t.length, t.data, t.id);
				toSend.removeLast();
			}
			else
				break;
		}
	}

	LinkedList<PendingTask> toSend = new LinkedList<PendingTask>();

	@Override
	public byte[] read(int unique_id) {
		AgentPort port = m_ports.get(new Integer(unique_id));
		if (port == null)
			Log.fatal("SimpleTCP: I'm confused; someone who's not attached is reading from a port");
		return port.packets.pollLast().toBytes();
	}

	private static class AgentPort {
		Agent agent;
		LinkedList<Packet> packets = new LinkedList<Packet>();

		public AgentPort(Agent a) {
			this.agent = a;
		}
	}

	private static class PendingTask {
		IPAddr src, dst;
		int length;
		byte[] data;
		int id;
	}
}
