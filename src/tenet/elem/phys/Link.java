package tenet.elem.phys;

import tenet.elem.Element;

public abstract class Link extends Element {

	/**
	 * Return the bandwidth of this link in bits per second (bps);
	 */
	public abstract int getBandwidth();

	/**
	 * Return the delay of this link in seconds (This will normally be a
	 * fraction though).
	 */
	public abstract double getDelay();

	public abstract Interface getIncomingInterface();

	public abstract Interface getOutgoingInterface();
}
