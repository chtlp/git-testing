package tenet.elem;

/**
 * Element is the abstract superclass of all the static elements in the
 * simulator, such as Nodes, Links, etc.
 */
public abstract class Element {

	protected int m_status;

	/**
	 * Ask the element to update itself.
	 */
	public abstract void update();

	/**
	 * Dump some descriptive information about the element for debugging
	 * purposes. This will most of the time display information about contained
	 * elements as well.
	 */
	public abstract void dump();

	public void setStatus(int status) {
		m_status = status;
	}

	public int getStatus(int status) {
		return m_status;
	}
}
