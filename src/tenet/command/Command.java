package tenet.command;

/**
 * Command is a very important class in JNS because it represents the commands
 * that the simulator is supposed to execute at a specific time.<br>
 * You have to subclass command to make use of it. You can then use it to
 * schedule a call to your class' foobar() function after x seconds.
 */
public abstract class Command implements Comparable<Command> {

	protected String m_name; // Name of the command (be artistic)
	protected double m_time; // The time at which the command should be executed

	/**
	 * Create a new command class with a specific name to execute at some
	 * specific time.
	 * 
	 * @param name
	 *            name of the command
	 * @param time
	 *            the time at which to execute the command
	 */
	public Command(String name, double time) {
		m_name = name;
		m_time = time;
	}

	/**
	 * Return the command's execution time.
	 */
	public double getTime() {
		return m_time;
	}

	/**
	 * Return the command's name.
	 */
	public String getName() {
		return m_name;
	}

	/**
	 * Debugging output, print's the command's name.
	 */
	public void dump() {
		System.out.println(m_name + ": " + m_time);
	}

	public int compareTo(Command c) {
		return (m_time - c.m_time < 0) ? -1 : (m_time - c.m_time > 0 ? 1 : 0);
	}

	/**
	 * The execution function. Has to be overridden by subclasses.
	 */
	public abstract void execute();
}
