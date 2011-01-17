package tenet.droute;

import java.util.ArrayList;

import tenet.core.Lib;
import tenet.core.Log;

public class OSPFPacket {
	public static final int HELLO = 0x1;
	public static final int LSA_UPDATE = 0x2;
	public static final int LSA_REQUEST = 0x4;
	public static final int LSA_ACK = 0x8;
	public static final int HELLO_ACK = 0x10;

	public int type;
	public int seqNumber;

	// for relaying a LSA update
	public int replyNumber;

	// for reply
	public int ackNumber;

	// public IPAddr srcIPAddr;
	int srcID;
	public ArrayList<LinkState> links;

	int metric;


	// used in HELLO & HELLO_ACK;
	double birth;

	boolean valid_packet = false;

	boolean isValid() {
		return valid_packet;
	}

	public OSPFPacket(byte[] data) {
		if (data.length < 16)
			return;
		if (Lib.bytesToInt(data, 0) != MyLib.calcChecksum(data, 4,
				data.length - 4))
			return;
		type = Lib.bytesToInt(data, 4);
		srcID = Lib.bytesToInt(data, 8);
		seqNumber = Lib.bytesToInt(data, 12);
		switch (type) {
		case HELLO:
		case HELLO_ACK:
			if (data.length == 28) {
				metric = Lib.bytesToInt(data, 16);
				birth = MyLib.bytesToDouble(data, 20);
				valid_packet = true;
			}
			break;
		case LSA_UPDATE:
			constructLSAUpdatePacket(data);
			break;
		case LSA_ACK:
			if (data.length >= 20) {
				ackNumber = Lib.bytesToInt(data, 16);
				valid_packet = true;
			}
			break;
		default:
			return;
		}

	}

	public OSPFPacket() {
	}

	public OSPFPacket(OSPFPacket other) {
		type = other.type;
		seqNumber = other.seqNumber;
		replyNumber = other.replyNumber;
		srcID = other.srcID;
		if (other.links != null)
			links = new ArrayList<LinkState>(other.links);
		metric = other.metric;
		birth = other.birth;
		ackNumber = other.ackNumber;
		valid_packet = other.valid_packet;
	}

	private void constructLSAUpdatePacket(byte[] data) {
		if (data.length <= 20 || (data.length - 20) % 13 != 0)
			return;
		links = new ArrayList<LinkState>();
		replyNumber = Lib.bytesToInt(data, 16);
		for (int i = 20; i < data.length; i += 13) {
			int ty = data[i];
			LinkState ls;
			if (ty == LinkState.TO_SUBNET) {
				int addr = Lib.bytesToInt(data, i + 1);
				int mask = Lib.bytesToInt(data, i + 5);

				int metric = Lib.bytesToInt(data, i + 9);
				ls = new LinkState(new IPSubnet(addr, mask), metric);
			}
			else {
				Lib.assertTrue(ty == LinkState.TO_ROUTER);
				int id = Lib.bytesToInt(data, i + 1);
				int metric = Lib.bytesToInt(data, i + 9);
				ls = new LinkState(id, metric);
			}
			links.add(ls);
		}
		valid_packet = true;
	}

	public static OSPFPacket createLSAUpdatePacket(int srcID, int seq,
			int reply,
			ArrayList<LinkState> links) {
		OSPFPacket ret = new OSPFPacket();
		ret.type = LSA_UPDATE;
		ret.srcID = srcID;
		ret.seqNumber = seq;
		ret.replyNumber = reply;
		ret.links = new ArrayList<LinkState>(links);
		return ret;
	}

	public static OSPFPacket createLSAACKPacket(int srcID, int seq, int ack) {
		OSPFPacket ret = new OSPFPacket();
		ret.type = LSA_ACK;
		ret.srcID = srcID;
		ret.seqNumber = seq;
		ret.ackNumber = ack;
		return ret;
	}

	public static OSPFPacket createHelloPacket(int srcID, int seq, int metric,
			double birth) {
		OSPFPacket ret = new OSPFPacket();
		ret.type = HELLO;
		ret.srcID = srcID;
		ret.seqNumber = seq;
		ret.metric = metric;
		ret.birth = birth;
		return ret;
	}

	public static OSPFPacket createHelloACKPacket(int srcID, int seq,
			int metric, double birth) {
		OSPFPacket ret = new OSPFPacket();
		ret.type = HELLO_ACK;
		ret.srcID = srcID;
		ret.seqNumber = seq;
		ret.metric = metric;
		ret.birth = birth;
		return ret;
	}

	public byte[] toBytes() {
		byte[] res;
		switch (type) {
		case LSA_UPDATE:
			res = new byte[20 + 13 * links.size()];
			fillInBasicInform(res);
			Lib.bytesFromInt(res, 16, replyNumber);
			int i = 20;
			for (LinkState ls : links) {
				if (ls.destRouterID >= 0) {
					res[i++] = LinkState.TO_ROUTER;
					Lib.bytesFromInt(res, i, ls.destRouterID);
				}
				else {
					res[i++] = LinkState.TO_SUBNET;
					Lib.bytesFromInt(res, i, ls.subnet.addr);
					Lib.bytesFromInt(res, i + 4, ls.subnet.mask);
				}
				Lib.bytesFromInt(res, i + 8, ls.metric);
				i += 12;
			}
			updateChecksum(res);
			return res;
		case HELLO:
		case HELLO_ACK:
			res = new byte[28];
			fillInBasicInform(res);
			Lib.bytesFromInt(res, 16, metric);
			MyLib.bytesFromDouble(res, 20, birth);
			updateChecksum(res);
			return res;
		case LSA_ACK:
			res = new byte[20];
			fillInBasicInform(res);
			Lib.bytesFromInt(res, 16, ackNumber);
			updateChecksum(res);
			return res;
		default:
			Log.warn("unknow OSPF Packet types" + type);
		}
		return null;
	}

	private void updateChecksum(byte[] data) {
		int c = MyLib.calcChecksum(data, 4, data.length - 4);
		Lib.bytesFromInt(data, 0, c);
	}

	private void fillInBasicInform(byte[] data) {
		Lib.bytesFromInt(data, 4, type);
		Lib.bytesFromInt(data, 8, srcID);
		Lib.bytesFromInt(data, 12, seqNumber);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("router=" + srcID + " seq=" + seqNumber + " ");
		switch (type) {
		case LSA_UPDATE:
			for (LinkState s : links)
				b.append("\n\t" + s);
			break;
		case HELLO:
			b.append("HELLO birth= " + birth);
			break;
		case HELLO_ACK:
			b.append("HELLO_ACK bith= " + birth);
			break;
		case LSA_ACK:
			b.append("ACK=" + ackNumber);
			break;
		}
		return b.toString();
	}

}
