package tenet.elem;

import tenet.elem.ip.IPAddr;

/**
 * Agent is the basic interface for any class that wants to act as an agent in
 * JNS.<br>
 * Agents are 'attached' to each other in JNS. Normally, the user will 'attach'
 * a higher-level agent to a lower-level agent (for example TCP to IP). This
 * agent will then call back the higher-level agent and 'attach' itself. So
 * there is a two-way attachment: user attaches higher-level to lower-level and
 * lower-level attaches itself to higher level (the latter process is
 * transparent to the outside user).
 */
public interface Agent {

	public final static int PACKET_AVAILABLE = 1;
	public final static int READY_TO_SEND = 2;

	// Values that will be indicated by connection-oriented agents
	public final static int CONNECTION_ESTABLISHED = 16;
	public final static int DISCONNECTED = 32;

	/**
	 * Attach a higher-level agent to this agent. You have to provide a unique
	 * identification number by which this agent can identify the higher-level
	 * agent; some of those are defined in jns.util.Protocols.
	 * 
	 * @param higher_level
	 *            the higher-level agent to attach to this agent
	 * @param unique_id
	 *            a unique identifier that will identify the higher- level agent
	 *            unambiguously
	 */
	public void attach(Agent higher_level, int unique_id);

	/**
	 * Attach a lower-level agent to this agent. This function is used as a
	 * callback. When a higher-level agent attaches itself to a lower level
	 * agent, the lower level agent will attach itself to the higher-level agent
	 * using this function.<br>
	 * The purpose of this is that the higher-level agent will implement this
	 * function in order to store some reference to the lower-level service
	 * which it can use to give packets to it.<br>
	 * Do NOT call this function as a user of the agent, only agents between
	 * themselves need to use it.
	 * 
	 * @param lower_level
	 *            the lower level agent to attach to this agent.
	 */
	public void attach(Agent lower_level);

	/**
	 * Indicate to this agent that a packet is waiting for collection. This is
	 * used by lower-level agents to indicate to higher-level agents. Note that
	 * higher-level agents may ignore any of the requests!
	 * 
	 * @param status
	 *            one of the constants defined above.
	 * @param indicator
	 *            the object that is indicating, normally an Agent, but could be
	 *            an Interface for example.
	 */
	public void indicate(int status, Object indicator);

	/**
	 * Query this agent if a packet of a given length could be sent. You must
	 * not call the 'send' function of any agent before calling this function.
	 * There are two possibilities:
	 * <ul>
	 * <li>The function returns 'true' - Go ahead and call 'send'
	 * <li>The function returns 'false' - In this case you are guaranteed to get
	 * a READY_TO_SEND indication by the lower level agent you are using. Do not
	 * try to send any packets but wait for a call to your indicate function
	 * with that flag set. When you do get that call, you <b>must</b> call
	 * canSend() again because someone else might take their turn before you!
	 * </ul>
	 * 
	 * @param destination
	 *            where to send the packet to
	 * @param length
	 *            the length of the data to be sent
	 */
	public boolean canSend(IPAddr destination, int length);
}
