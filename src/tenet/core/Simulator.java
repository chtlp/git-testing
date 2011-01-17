package tenet.core;

import java.util.Vector;
import java.util.Enumeration;
import java.util.PriorityQueue;

import tenet.command.*;
import tenet.elem.Element;

/**
 * Simulator is the main class in JNS. It contains the main loop that will
 * update the elements of the network. Objects can request to have their own
 * commands scheduled here to have them executed at a specific time.
 */
public class Simulator {

	private static ISystemWatcher swatcher = null;
	
	private static Simulator m_instance = null;

	private Vector<Element> m_elements;

	private double m_time; // Time in seconds

	private double time_limit = -1;

	private boolean m_finished; // Flag to finish the run() loop

	private PriorityQueue<Command> m_commands; // Queue of Command objects

	static {
		Config.load("tenet.conf");
		Log.setupLog(Config.getString("system.LogLevel", ""),
				Config.getString("system.LogFlags", ""));
		Lib.enableAssertion(Config.getBoolean("system.EnableAssertion", true));
	}

	public void setSystemWatcher(ISystemWatcher watcher) {
		swatcher=watcher;
	}
	
	/**
	 * Default Constructor. Initialize the internal structure and resets the
	 * time to zero. You cannot call this from outside, this is a singleton
	 * class. Use the getInstance function.
	 * 
	 * @see getInstance
	 */
	private Simulator() {
		m_elements = new Vector<Element>();
		m_commands = new PriorityQueue<Command>();
		m_time = 0;
		m_finished = false;
	}

	/**
	 * Return the singleton instance of the simulator. There can only ever be
	 * one simulator active at a time.
	 * 
	 * @return the one and only simulator object
	 */
	public static Simulator getInstance() {
		if (m_instance == null)
			m_instance = new Simulator();
		return m_instance;
	}

	/**
	 * Reset the singleton instance
	 */
	public static void resetInstance() {
		m_instance = null;
	}

	/**
	 * Set the overall simulation time of this simulator
	 * 
	 * @param t
	 *            simulation time
	 */
	public void setTimeLimit(double t) {
		time_limit = t;
	}

	/**
	 * Attach a new element to the simulator to be updated regularly during the
	 * simulation.
	 * 
	 * @param element
	 *            the element to add
	 */
	public void attach(Element element) {
		m_elements.addElement(element);
	}

	/**
	 * Add a command the simulator should execute at a specific time.
	 * 
	 * @param command
	 *            the command object that should be executed
	 */
	public void schedule(Command command) {
		if(swatcher!=null)
		{
			Exception e=new Exception();
			swatcher.eventRecord(command,e.getStackTrace());
		}
		if (time_limit == -1 || command.getTime() < time_limit)
			m_commands.add(command);
	}

	/**
	 * Dump the contents of the simulator, for debugging purposes. This function
	 * calls the dump function of all elements in the simulator.
	 */
	public void dump() {

		System.out.println("----------[STATIC ELEMENTS]----------");

		Enumeration<Element> e = m_elements.elements();
		while (e.hasMoreElements()) {
			Element curelement = (Element) e.nextElement();

			curelement.dump();
			System.out.println("---------------");
		}

		System.out.println("------------[COMMANDS]---------------");
		for (Command curcommand : m_commands) {
			System.out.println(curcommand.getName() + ": "
					+ curcommand.getTime());
		}
	}

	/**
	 * Enumerate the elements stored in the simulator. This can be used by other
	 * objects to, say, print them out.
	 * 
	 * @return an Enumeration of the elements in the simulator
	 */
	public Enumeration<Element> enumerateElements() {
		return m_elements.elements();
	}

	/**
	 * Run the actual simulation. This takes over control from the calling
	 * program for a long period of time, don't expect to do anything in the
	 * meantime. After the execution of run(), the contents of the simulator and
	 * all its elements are invalid.
	 */
	public void run() {
		long time = System.currentTimeMillis();

		int counter = 0;
		while (m_commands.size() > 0 && !m_finished) {
			++counter;
			Command current_command = (Command) m_commands.peek();
			m_commands.poll();

			m_time = current_command.getTime();
			current_command.execute();
			if (m_finished) {
				Log.debug('N', "finished");
			}
		}

		System.out.println("Total simulation time: "
				+ (System.currentTimeMillis() - time) + " ms");
	}

	/**
	 * Return the current time in the simulation, in seconds.
	 * 
	 * @return the current time in seconds
	 */
	public double getTime() {
		return m_time;
	}

	/**
	 * Set a flag that will quit the simulator immediately.
	 */
	public void setFinished() {
		m_finished = true;
	}
}
