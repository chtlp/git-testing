package tenet.test.stcptest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeMap;

import tenet.command.Command;
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
public class STCPTestSink implements Agent {

	ConnectionOrientedAgent m_transport;
	IPAddr m_local_ip;
	IPAddr m_remote_ip;

	// Start and stop time
	double m_start;
	double m_end;

	ArrayList<Integer> goal;
	ArrayList<Integer> result;

	/**
	 * Create a new random sink that will start listening for incoming
	 * connections at some point and stop listening at some other point.
	 * 
	 * @param ip
	 *            the ip address of this sink
	 * @param start_time
	 *            the time at which to start listening
	 * @param stop_time
	 *            the time at which to stop listening
	 */
	public STCPTestSink(IPAddr ip, double start_time, double stop_time,
			LinkedList<Integer> arr) {
		m_local_ip = ip;
		m_remote_ip = null;

		m_start = start_time;
		m_end = stop_time;

		goal = new ArrayList<Integer>();
		goal.addAll(arr);
		result = new ArrayList<Integer>();

		// Schedule a new command that will start listening at 'start time'

		Simulator.getInstance().schedule(
				new Command("RandomSinkStart", m_start) {
					@Override
					public void execute() {
						m_transport.listen(m_local_ip);
					}
				});

		// Schedule another command that will take down the connection at
		// 'stop_time' (if there is a connection)

		Simulator.getInstance().schedule(new Command("RandomSinkStop", m_end) {
			@Override
			public void execute() {
				int[][] f = new int[goal.size() + 1][];
				for (int i = 0; i <= goal.size(); ++i) {
					f[i] = new int[result.size() + 1];
					f[i][0] = 0;
				}
				for (int j = 0; j <= result.size(); ++j) {
					f[0][j] = 0;
				}
				for (int i = 1; i <= goal.size(); ++i) {
					for (int j = 1; j <= result.size(); ++j) {
						f[i][j] = Math.max(f[i - 1][j], f[i][j - 1]);
						if (goal.get(i - 1).equals(result.get(j - 1))) {
							f[i][j] = Math.max(f[i - 1][j - 1] + 1, f[i][j]);
						}
					}
				}
				System.out.println("s: " + f[goal.size()][result.size()]);
				// m_transport.disconnect();
			}
		});
	}

	/**
	 * Do nothing. Nobody can attach to this agent and use it.
	 */
	@Override
	public void attach(Agent higher_level, int unique_id) {
	}

	@Override
	public void attach(Agent lower_level) {
		m_transport = (ConnectionOrientedAgent) lower_level;
	}

	@Override
	public void indicate(int status, Object indicator) {
		if (status == Agent.CONNECTION_ESTABLISHED) {
			System.out.println("STCPTest sink connected");

			Simulator.getInstance().schedule(
					new STCPTestSinkCommand(
							Simulator.getInstance().getTime() + 0.001, this));
		}
		else if (status == Agent.PACKET_AVAILABLE) {
			Simulator.getInstance().schedule(
					new STCPTestSinkCommand(
							Simulator.getInstance().getTime() + 0.001, this));
		}
	}

	/**
	 * This is a 'sink' that is there to receive packets, not send them. Thus
	 * this function always returns false.
	 * 
	 * @param destination
	 *            ignored
	 * @param length
	 *            ignored
	 * @return false
	 */
	@Override
	public boolean canSend(IPAddr destination, int length) {
		return false;
	}

	TreeMap<Integer, Integer> received = new TreeMap<Integer, Integer>();

	public void record(Integer num) {
		Integer k = received.get(num);
		k = k != null ? k : 0;
		received.put(num, k + 1);
	}

	public void report() {
		for (Integer k : received.keySet()) {
			System.out.println("Report " + k + " : " + received.get(k));
		}
	}

}

class STCPTestSinkCommand extends Command {

	private STCPTestSink m_sink;

	public STCPTestSinkCommand(double time, STCPTestSink sink) {
		super("RandomSinkRead", time);
		m_sink = sink;
	}

	@Override
	public void execute() {
		byte[] p = m_sink.m_transport.read(0);
		if (p == null || p.length == 0)
			return;
		// assert p.length == 4 : "Length = " + p.length;
		Lib.assertTrue(p.length % 4 == 0);
		// DEBUG
		// int data = bytes2int(p);

		for (int i = 0; i < p.length; i += 4) {
			int data = Lib.bytesToInt(p, i);
			m_sink.result.add(data);
			m_sink.record(data);

			System.out.println("received " + data + " at time "
					+ Simulator.getInstance().getTime());
		}

	}

	private int bytes2int(byte[] b) {
		int mask = 0xff;
		int temp = 0;
		int res = 0;
		for (int i = 0; i < 4; i++) {
			res <<= 8;
			temp = b[i] & mask;
			res |= temp;
		}
		return res;
	}

}
