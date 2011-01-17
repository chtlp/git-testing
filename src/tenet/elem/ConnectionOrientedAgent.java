package tenet.elem;

import tenet.elem.ip.IPAddr;

/**
 * CO_Agent is JNS' minimum interface for an agent that wants to provide a
 * connection-oriented (but not necessarily reliable) service.
 */
public interface ConnectionOrientedAgent extends Agent {

	/**
	 * Set up a connection to a specific node at a specific port. This function
	 * has to be used before data transfer can take place. A source IP also has
	 * to be provided because any node can have more than one IP address. Note
	 * that it will take time to set up a connection but when it is done you
	 * will receive a call to your 'indicate' function with
	 * CONNECTION_ESTABLISHED as a parameter.
	 * 
	 * @param source
	 *            the IP to send from
	 * @param destination
	 *            the destination IP to send to
	 * @param destination_port
	 *            a unique identifier at the destination that has a counterpart
	 *            of this agent listening at it.
	 */
	public void connect(IPAddr source, IPAddr destination, int destination_port);

	/**
	 * Break up a connection. No parameters are required because in JNS a
	 * CO_Agent only has one connection open at a time. (Although many such
	 * agents might be managed by another agent...)
	 */
	public void disconnect();

	/**
	 * Listen for incoming connection requests. When a connection request comes
	 * in, this will always accept it (unlike the normal call to 'accept' you
	 * have to do in the real world).<br>
	 * Also note that you do not have to specify a port number because that
	 * should be specified when the agent is created. You do have to provide the
	 * local IP address, however.
	 */
	public void listen(IPAddr local_ip);

	/**
	 * Send some data to a remote node. This will only work if a connection has
	 * been established first.
	 * 
	 * @param length
	 *            the length of the data to be sent in bytes
	 * @param data
	 *            the object to be sent
	 * @param unique_id
	 *            if a higher-level agent is using this agent to send data, this
	 *            should identify the agent so that data can be given to its
	 *            counter-part at the other end using this identifier
	 */
	public void send(int length, byte[] data, int unique_id);

	/**
	 * Read data from this agent. If data is not available, null might be
	 * returned.
	 * 
	 * @param unique_id
	 *            the unique identifier that identifies the higher-level service
	 *            using this agent.
	 */
	public byte[] read(int unique_id);

	/**
	 * Ask whether connect is still alive.
	 * 
	 * @return true if connected.
	 */
	public boolean isConnected();
}
