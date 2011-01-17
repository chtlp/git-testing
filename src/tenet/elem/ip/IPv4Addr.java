package tenet.elem.ip;

public class IPv4Addr implements IPAddr {
	public static IPv4Addr newInstance(int v) {
		return new IPv4Addr(v);
	}

	public static IPv4Addr newInstance(int a0, int a1, int a2, int a3) {
		return new IPv4Addr(((a0 & 0xff) << 24) | ((a1 & 0xff) << 16)
				| ((a2 & 0xff) << 8) | (a3 & 0xff));
	}

	public static IPv4Addr newInstance(String ip) {
		String[] addr = ip.split("\\.");
		int code = 0;
		for (int i = 0; i < 4; ++i)
			code = (code << 8) | (new Integer(addr[i]) & 0xff);
		return new IPv4Addr(code);
	}

	public static IPv4Addr newInstance(IPAddr ip) {
		return (IPv4Addr) ip;
	}

	private int code;

	private IPv4Addr(int code) {
		this.code = code;
	}

	public int hashCode() {
		return this.toString().hashCode();
	}

	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other instanceof IPv4Addr)
			return code == ((IPv4Addr) other).code;
		return false;
	}

	@Override
	public String toString() {
		return ((code >> 24) & 0xff) + "." + ((code >> 16) & 0xff) + "."
				+ ((code >> 8) & 0xff) + "." + (code & 0xff);
	}

	@Override
	public int toInt() {
		return code;
	}
}
