package tenet.elem.ip;

/**
 * An interface for IP Address, works both for IPv4 and IPv6.
 * 
 * @author WuCY
 */
public interface IPAddr {
	/**
	 * Stub method for IPv4 addresses, not suitable for IPv6 addresses.
	 * 
	 * @return the integer value of this ip address
	 */
	int toInt();
}
