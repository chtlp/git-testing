package tenet.stcp;

import java.util.zip.CRC32;

import tenet.core.Config;
import tenet.core.Lib;
import tenet.elem.Packet;

public final class SimpleTCPPacket implements Packet {
	public static final int CWR = 0x1, ECE = 0x2, URG = 0x4, ACK = 0x8,
			PSH = 0x10, RST = 0x20, SYN = 0x40, FIN = 0x80, FINACK = 0x100;
	// Header size in bytes
	public final static int HEADER_SIZE = 32;

	// Source port number
	private int source_port;

	// Destination port number
	private int destination_port;

	// Sequence number
	private int sequence;

	// ACK number
	private int ack;

	// Total length of this packet in bytes (including header)
	private int length;

	// Flags consisting of the constants defined above (for connection
	// setup and teardown)
	private int flag;

	// for flow control
	private int windowSize;

	// for data integrity
	private int checksum;

	private byte[] data;

	// public int sentTimes = 1;
	// public double lastSentTime = 0.0;
	// public double expireInterval = 0.0;

	private static int WINDOWSIZE = -1;

	// private int calcChecksum() {
	// int c;
	// c = 0;
	// c = c ^ source_port;
	// c = c ^ destination_port;
	// c = c ^ sequence;
	// c = c ^ ack;
	// c = c ^ length;
	// c = c ^ flag;
	// c = c ^ windowSize;
	//
	// for (int i = 0; i < data.length; ++i) {
	// c = c ^ (data[i] << ((i % 4) * 8));
	// }
	// return c;
	// }

	private int calcChecksum() {
		CRC32 crc = new CRC32();
		byte[] b = new byte[HEADER_SIZE - 4 + data.length];
		Lib.bytesFromInt(b, 0, source_port);
		Lib.bytesFromInt(b, 4, destination_port);
		Lib.bytesFromInt(b, 8, sequence);
		Lib.bytesFromInt(b, 12, ack);
		Lib.bytesFromInt(b, 16, length);
		Lib.bytesFromInt(b, 20, flag);
		Lib.bytesFromInt(b, 24, windowSize);
		for (int i = 0; i < data.length; ++i)
			b[HEADER_SIZE - 4 + i] = data[i];
		crc.update(b);
		return (int) crc.getValue();
	}

	public void updateChecksum() {
		checksum = calcChecksum();
	}

	public boolean verifyChecksum() {
		return checksum == calcChecksum();
	}

	public SimpleTCPPacket() {
		source_port = destination_port = 0;
		sequence = ack = 0;
		flag = 0;
		windowSize = WINDOWSIZE < 0 ? Config.getInteger("stcp.receiverWindow")
				: WINDOWSIZE;
		data = new byte[0];
		length = HEADER_SIZE;
	}

	public SimpleTCPPacket(SimpleTCPPacket that) {
		this.source_port = that.source_port;
		this.destination_port = that.destination_port;
		this.ack = that.ack;
		this.sequence = that.sequence;
		this.data = that.data;
		this.flag = that.flag;
		this.length = that.length;
	}

	public int getSourcePort() {
		return source_port;
	}

	public void setSourcePort(int port) {
		source_port = port;
	}

	public int getDestinationPort() {
		return destination_port;
	}

	public void setDestinationPort(int port) {
		destination_port = port;
	}

	public int getACK() {
		return ack;
	}

	public void setACK(int ack) {
		this.ack = ack;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public int getFlag() {
		return flag;
	}

	public void setFlag(int flag) {
		this.flag = flag;
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public void setData(byte[] data) {
		this.data = data;
		this.length = HEADER_SIZE + data.length;
	}

	public int getLength() {
		return length;
	}

	@Override
	public byte[] toBytes() {
		updateChecksum();
		byte[] b = new byte[length];
		Lib.bytesFromInt(b, 0, source_port);
		Lib.bytesFromInt(b, 4, destination_port);
		Lib.bytesFromInt(b, 8, sequence);
		Lib.bytesFromInt(b, 12, ack);
		Lib.bytesFromInt(b, 16, length);
		Lib.bytesFromInt(b, 20, flag);
		Lib.bytesFromInt(b, 24, windowSize);
		Lib.bytesFromInt(b, 28, checksum);
		for (int i = 0; i < data.length; ++i)
			b[HEADER_SIZE + i] = data[i];
		return b;
	}

	@Override
	public boolean fromBytes(byte[] b) {
		source_port = Lib.bytesToInt(b, 0);
		destination_port = Lib.bytesToInt(b, 4);
		sequence = Lib.bytesToInt(b, 8);
		ack = Lib.bytesToInt(b, 12);
		length = Lib.bytesToInt(b, 16);
		flag = Lib.bytesToInt(b, 20);
		windowSize = Lib.bytesToInt(b, 24);
		checksum = Lib.bytesToInt(b, 28);
		data = new byte[b.length - HEADER_SIZE];
		for (int i = 0; i < data.length; ++i)
			data[i] = b[HEADER_SIZE + i];
		return verifyChecksum();
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public boolean hasFIN() {
		return (flag & FIN) > 0;
	}

	public boolean hasSYN() {
		return (flag & SYN) > 0;
	}

	public boolean hasACK() {
		return (flag & ACK) > 0;
	}

	public SimpleTCPPacket setFIN() {
		flag = flag | FIN;
		return this;
	}

	public SimpleTCPPacket setSYN() {
		flag = flag | SYN;
		return this;
	}

	public SimpleTCPPacket setACK() {
		flag = flag | ACK;
		return this;
	}

	public boolean hasRST() {
		return (flag & RST) > 0;
	}

	// public void setFINACK() {
	// flag = flag | FINACK;
	// }
	//
	// public boolean hasFINACK() {
	// return (flag & FINACK) > 0;
	// }

}
