package tenet.droute;

import java.util.ArrayList;

import tenet.command.Command;
import tenet.core.Lib;
import tenet.core.Simulator;
import tenet.elem.ip.IPPacket;
import tenet.elem.ip.IPv4Packet;

public class MyLib {
	/*
	 * consecutive positive bits from higher position
	 */
	public static int cpbfhp(int mask) {
		for (int i = 31; i >= 0; --i) {
			if ((mask & (1 << i)) == 0)
				return 31 - i;
		}
		return 32;
	}

	public static double currentTime() {
		return Simulator.getInstance().getTime();
	}

	public static void schedule(Command cmd) {
		Simulator.getInstance().schedule(cmd);
	}

	public static int calcChecksum(byte[] data, int offset, int length) {
		int res = 0;
		for (int i = offset; i < offset + length; ++i) {
			res ^= data[i] << (i % 4 * 8);
		}
		return res;
	}

	/**
	 * a special function to compare the relative largeness of two sequence
	 * numbers
	 * 
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	public static int seqCmp(int lhs, int rhs) {
		long d1 = lhs - rhs;
		long d2 = (long) (lhs - Integer.MAX_VALUE)
				+ (long) (Integer.MIN_VALUE - rhs) - 1;
		return (int) (Math.abs(d1) < Math.abs(d2) ? d1 : d2);
	}

	public static int divRoundUp(int x, int y) {
		if (x % y == 0)
			return x / y;
		else
			return x / y + 1;
	}

	public static int remRoundUp(int x, int y) {
		int r = x % y;
		return r == 0 ? y : r;
	}

	// NOTE ippacket should reveal this
	static final int HEADER_SIZE = 6 * 4;

	public static ArrayList<IPPacket> fragment(IPPacket ipp, int mtu) {
		int more = ipp.getHeader(IPv4Packet.FLAG_MORE_FRAGMENTS) > 0 ? 1 : 0;
		int offset = ipp.getHeader(IPv4Packet.FRAGMENT_OFFSET);
		int id = ipp.getHeader(IPv4Packet.IDENTIFICATION);
		ArrayList<IPPacket> res = new ArrayList<IPPacket>();
		int n = divRoundUp(ipp.getData().length, mtu - HEADER_SIZE);
		byte[] whole = ipp.getData();
		for (int i = 0; i < n; ++i) {
			int upper = i + 1 == n ? remRoundUp(whole.length, mtu - HEADER_SIZE)
					: mtu - HEADER_SIZE;
			byte[] data = new byte[upper];
			for (int j = 0; j < upper; ++j)
				data[j] = whole[j + i * (mtu - HEADER_SIZE)];
			IPv4Packet p = new IPv4Packet((IPv4Packet) ipp);
			p.setData(data);
			p.setHeader(IPv4Packet.IDENTIFICATION, id);
			p.setHeader(IPv4Packet.FRAGMENT_OFFSET, offset + i
					* (mtu - HEADER_SIZE));
			p.setHeader(IPv4Packet.FLAG_MORE_FRAGMENTS, i + 1 < n ? 1 : more);
			res.add(p);
		}
		// for (IPPacket p : res) {
		// Log.debug('G', "frag length=" + p.getData().length);
		// }
		return res;
	}

	public static double bytesToDouble(byte[] array, int offset) {
		long l = 0;
		for (int i = 0; i < 8; ++i)
			l = l | ((long) array[offset + 0] & 0xFF) << (i * 8);
		return Double.doubleToLongBits(l);
	}

	public static void bytesFromDouble(byte[] array, int offset, double dvalue) {
		long value = Double.doubleToLongBits(dvalue);
		for (int i = 0; i < 8; ++i)
			array[offset + i] = (byte) ((value >> (i * 8)) & 0xFF);
	}

	public static String[] chopThree(String str) {
		Lib.assertTrue(str.length() % 3 == 0);
		String[] res = new String[str.length() / 3];
		for (int i = 0; i < res.length; ++i)
			res[i] = str.substring(i * 3, i * 3 + 3);
		return res;
	}

}
