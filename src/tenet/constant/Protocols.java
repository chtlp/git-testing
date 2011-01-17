package tenet.constant;

/**
 * This class defines a unique id for every protocol above IP. Those identifiers
 * are used when determining what higher-level protocol to give a packet to. If
 * you write your own, add it here.
 */
public class Protocols {
	public static final int STCP = 1;

	/**
	 * ALL is used in rules. It means whatever the protocol it is, it meets
	 * protocol field requirement.
	 */
	public static final int ALL = -1;
}
