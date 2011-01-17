package tenet.elem;

import tenet.elem.ip.IPAddr;

/**
 * CL_Agent is the minimum interface of any agent in JNS that wants to provide
 * connection-less packet delivery.
 */
public interface ConnectionLessAgent extends Agent {

	/**
	 * Tell the connection-less agent to send a packet of length 'length' from
	 * source to destination. The source field is present because a given host
	 * might have more than one IP address. You also have to provide the id of
	 * your agent by which this agent can identify you (you have to use the
	 * 'attach' function from Agent first and provide the same ID that you used
	 * there).
	 * 
	 * @param source
	 *            the source IP address, this hosts primary address
	 * @param dest
	 *            the destination IP address
	 * @param length
	 *            the length of the packet to be sent
	 * @param data
	 *            an object carrying the data of the packet
	 * @param unique_id
	 *            the id that identifies your agent when you give a packet to
	 *            this agent. It has to be the same one you used for 'attach'.
	 * @see tenet.elem.Agent
	 */
	public void send(IPAddr source, IPAddr dest, int length, byte[] data,
			int unique_id);

	/**
	 * Read an object from this agent, if there is an object awaiting
	 * collection. This function will return null otherwise.
	 * 
	 * @return an object (probably a higher level packet) or null if there is no
	 *         packet waiting
	 */
	public byte[] read(int unique_id);
}
