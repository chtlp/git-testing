package tenet.elem.ip;

import tenet.core.Lib;

/**
 * An implementation of the IPv4 Packet.
 * 
 */
public class IPv4Packet implements IPPacket {
	public static final int VERSION = 0;
	public static final int IHL = 1;
	public static final int TYPE_OF_SERVICE = 2;
	public static final int TOTAL_LENGTH = 3;
	public static final int IDENTIFICATION = 4;
	public static final int FLAGS = 5;
	public static final int FRAGMENT_OFFSET = 6;
	public static final int TIME_TO_LIVE = 7;
	public static final int PROTOCOL = 8;
	public static final int CHECKSUM = 9;
	public static final int SOURCE_IP = 10;
	public static final int DEST_IP = 11;
	public static final int OPTIONS = 12;

	public static final int FLAG_MORE_FRAGMENTS = 2;

	private static final int HEADER_SIZE = 6;

	private final static int[][] FIELD_FORMAT = { { 0, 0, 15 }, { 0, 4, 15 },
			{ 0, 8, (1 << 8) - 1 }, { 0, 16, (1 << 16) - 1 },
			{ 1, 0, (1 << 16) - 1 }, { 1, 16, (1 << 3) - 1 },
			{ 1, 19, (1 << 13) - 1 }, { 2, 0, (1 << 8) - 1 },
			{ 2, 8, (1 << 8) - 1 }, { 2, 16, (1 << 16) - 1 }, { 3, 0, -1 },
			{ 4, 0, -1 }, { 5, 0, -1 } };

	private int[] header;
	private boolean hasOptions;
	private byte[] data;

	public IPv4Packet() {
		hasOptions = false;
		header = new int[HEADER_SIZE];

		setHeader(VERSION, 4);
		setHeader(IHL, 5);
		setHeader(TOTAL_LENGTH, 20);
	}

	public IPv4Packet(IPv4Packet that) {
        this.header = new int[HEADER_SIZE];
		System.arraycopy(that.header, 0, this.header, 0, this.header.length);
		this.data = that.data;
        this.hasOptions = that.hasOptions;
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public void setData(byte[] data) {
		this.data = data;
		setHeader(TOTAL_LENGTH, getHeader(IHL) * 4 + data.length);
	}

	@Override
	public int getHeader(int type) {
		Lib.assertTrue(type >= VERSION && type <= OPTIONS);
		final int[] itv = FIELD_FORMAT[type];
		return Lib.getBits(header[itv[0]], itv[1], itv[2]);
	}

	@Override
	public void setHeader(int type, int value) {
		Lib.assertTrue(type >= VERSION && type <= OPTIONS);
		if (type == OPTIONS && !hasOptions) {
			hasOptions = true;
			setHeader(IHL, 6);
			setHeader(TOTAL_LENGTH, 24 + data.length);
		}

		final int[] itv = FIELD_FORMAT[type];
		Lib.assertTrue(itv[2] == -1 || value >= 0 && value <= itv[2]);
		header[itv[0]] = Lib.setBits(header[itv[0]], value, itv[1], itv[2]);

		if (type != CHECKSUM) {
			setHeader(CHECKSUM, 0);
			setHeader(CHECKSUM, calcCRC());
		}
	}

	@Override
	public int getVersion() {
		return getHeader(VERSION);
	}

	@Override
	public int getLength() {
		return getHeader(TOTAL_LENGTH);
	}

	@Override
	public IPAddr getSourceIP() {
		return IPv4Addr.newInstance(this.getHeader(SOURCE_IP));
	}

	@Override
	public IPAddr getDestIP() {
		return IPv4Addr.newInstance(this.getHeader(DEST_IP));
	}

	@Override
	public void setSourceIP(IPAddr ip) {
		this.setHeader(SOURCE_IP, ip.toInt());
	}

	@Override
	public void setDestIP(IPAddr ip) {
		this.setHeader(DEST_IP, ip.toInt());
	}

	@Override
	public boolean checkCRC() {
		return calcCRC() == 0;
	}

	private final int calcCRC() {
		final int mask = (1 << 16) - 1;
		int crc = 0;
		for (int i = 0; i < HEADER_SIZE; ++i)
			if (i < 5 || hasOptions)
				crc += (header[i] & mask) + (header[i] >>> 16);
		while (crc >= (1 << 16))
			crc = (crc & mask) + (crc >>> 16);
		return ~crc & mask;
	}

	@Override
	public byte[] toBytes() {
		final int length = hasOptions ? 24 : 20;
		byte[] bytes = new byte[length + (data == null ? 0 : data.length)];
		for (int i = 0; i < length; i += 4)
			Lib.bytesFromInt(bytes, i, header[i >> 2]);
		if (data != null)
			System.arraycopy(data, 0, bytes, length, data.length);
		return bytes;
	}

	@Override
	public boolean fromBytes(byte[] bytes) {
		header[0] = Lib.bytesToInt(bytes, 0);
		if (getHeader(VERSION) != 4)
			return false;
		for (int i = 1; i < 5; ++i)
			header[i] = Lib.bytesToInt(bytes, i * 4);

		final int hLen = getHeader(IHL) * 4;
		if (hLen == 24) {
			header[5] = Lib.bytesToInt(bytes, 20);
			hasOptions = true;
		} else
			hasOptions = false;
		if (bytes.length < hLen)
			return false;
		if (bytes.length == hLen)
			data = null;
		else {
			data = new byte[bytes.length - hLen];
			System.arraycopy(bytes, hLen, data, 0, data.length);
		}
		return true;
	}
}
