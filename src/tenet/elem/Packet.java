package tenet.elem;

public interface Packet {
	/**
	 * Get the data of this packet
	 * 
	 * @return the data inside this packet
	 */
	byte[] getData();

	/**
	 * Set the data of this packet
	 */
	void setData(byte[] data);

	/**
	 * Convert this packet to binary data
	 * 
	 * @return the binary data
	 */
	byte[] toBytes();

	/**
	 * Construct this packet from binary data
	 * 
	 * @param bytes
	 *            the binary data
	 * @return true if the construction was successful
	 */
	boolean fromBytes(byte[] bytes);
}
