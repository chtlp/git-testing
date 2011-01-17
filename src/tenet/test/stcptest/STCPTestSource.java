package tenet.test.stcptest;

import java.util.LinkedList;

import tenet.command.Command;
import tenet.core.Config;
import tenet.core.Lib;
import tenet.core.Simulator;
import tenet.elem.Agent;
import tenet.elem.ConnectionOrientedAgent;
import tenet.elem.ip.IPAddr;

/**
 * RandomSource is an application layer agent that can run on top of a
 * connection-oriented transport layer protocol (like SGN, TCP, ...). It is used
 * in conjunction with RandomSink and sends data of random length at random time
 * intervals.<br>
 * It communicates at port 80.
 */
public class STCPTestSource implements Agent {

	ConnectionOrientedAgent m_transport;
	IPAddr m_source_ip;
	IPAddr m_dest_ip;

	// Start and stop time
	double m_start;
	double m_end;

	LinkedList<Integer> list;

	/**
	 * Create a new random source that will start sending at some point and stop
	 * sending at some other point.
	 * 
	 * @param source
	 *            the source IP from which to send (one of the IP addresses of
	 *            the node this is at).
	 * @param dest
	 *            the IP to send to. Make sure this IP has a RandomSink attached
	 *            and listening before you start sending!
	 * @param start_time
	 *            the time at which to start sending
	 * @param stop_time
	 *            the time at which to stop sending
	 */
	public STCPTestSource(IPAddr source, IPAddr dest, double start_time,
			double stop_time, LinkedList<Integer> arr) {
		m_source_ip = source;
		m_dest_ip = dest;

		m_start = start_time;
		m_end = stop_time;

		list = new LinkedList<Integer>();
		list.addAll(arr);

		// Schedule a new command that will set up a connection at 'start_time'

		Simulator.getInstance().schedule(
				new Command("RandomSourceStart", m_start) {
					@Override
					public void execute() {
						m_transport.connect(m_source_ip, m_dest_ip, 80);
					}
				});

		// Schedule another command that will take down the connection at
		// 'stop_time'

		Simulator.getInstance().schedule(
				new Command("RandomSourceStop", m_end) {
					@Override
					public void execute() {
						m_transport.disconnect();
					}
				});

	}

	/**
	 * Do nothing. Nobody can attach to this agent and use it.
	 */
	public void attach(Agent higher_level, int unique_id) {
	}

	public void attach(Agent lower_level) {
		m_transport = (ConnectionOrientedAgent) lower_level;
	}

	public void indicate(int status, Object indicator) {
		if (status == Agent.CONNECTION_ESTABLISHED) {
			System.out.println("STCPTest source connected");

			Simulator.getInstance().schedule(
					new STCPTestSourceCommand(
							Simulator.getInstance().getTime() + 0.001, this));
		}
	}

	/**
	 * RandomSource is supposed to be a top-level agent. It sends data by itself
	 * and no one is supposed to use it to send data. Thus this function will
	 * always return 'false'.
	 * 
	 * @param destination
	 *            ignored
	 * @param length
	 *            ignored
	 * @return false
	 */
	public boolean canSend(IPAddr destination, int length) {
		return false;
	}

}

class STCPTestSourceCommand extends Command {

	STCPTestSource m_source;

	public STCPTestSourceCommand(double time, STCPTestSource source) {
		super("RandomSourceSend", time);
		m_source = source;
	}

	@Override
	public void execute() {
		if (m_source.m_transport.canSend(null, 0)) {

			int num = m_source.list.remove();
			byte[] data = new byte[4];
			Lib.bytesFromInt(data, 0, num);
			m_source.m_transport.send(4, data, 0);
			System.out.println("sent " + num + " at time "
					+ Simulator.getInstance().getTime());
		}
		if (!m_source.list.isEmpty())
			Simulator.getInstance().schedule(
					new STCPTestSourceCommand(Simulator.getInstance().getTime()
							+ Config.getDouble("tester.interval"), m_source));
	}

	private byte[] int2bytes(int num) {
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			b[i] = (byte) (num >>> (24 - i * 8));
		}
		return b;
	}

}
