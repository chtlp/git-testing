package tenet.elem.ip;

import tenet.elem.Packet;

/**
 * The IPPacket interface provides a set of interface which can be compatible
 * with both IPv4 and IPv6.
 * 
 */
public interface IPPacket extends Packet {
	/**
	 * Returns the version of this packet
	 * 
	 * @return
	 */
	int getVersion();

	/**
	 * Returns the total length of this packet
	 * 
	 * @return
	 */
	int getLength();

	/**
	 * Returns the header of specific type
	 * 
	 * @param type
	 * @return
	 */
	int getHeader(int type);

	/**
	 * Set the header of specific type to some value
	 * 
	 * @param type
	 * @param value
	 * @return
	 */
	void setHeader(int type, int value);

	/**
	 * A routine for getting the source IP address
	 * 
	 * @return
	 */
	IPAddr getSourceIP();

	/**
	 * A routine for setting the source IP address
	 * 
	 * @return
	 */
	void setSourceIP(IPAddr ip);

	/**
	 * A routine for getting the destination IP address
	 * 
	 * @return
	 */
	IPAddr getDestIP();

	/**
	 * A routine for setting the destination IP address
	 * 
	 * @return
	 */
	void setDestIP(IPAddr ip);

	/**
	 * Check the integrity of the packet
	 * 
	 * @return
	 */
	boolean checkCRC();

}
