package tenet.droute;

import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPv4Addr;

class IPSubnet {
	public int addr = 0x00000000;
	public int mask = 0xFFFFFFFF;
	
	public IPSubnet(int a, int m) {
		addr = a;
		mask = m;
	}
	
	public IPSubnet(IPAddr ipa) {
		addr = ipa.toInt();
	}
	
	public int match(IPAddr other) {
		return (other.toInt() & mask) == addr ? MyLib.cpbfhp(mask) : -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IPSubnet) {
			IPSubnet other = (IPSubnet) obj;
			return addr == other.addr && mask == other.mask;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return addr ^ mask;
	}

	@Override
	public String toString() {
		return IPv4Addr.newInstance(addr) + " " + IPv4Addr.newInstance(mask);
	}

}
