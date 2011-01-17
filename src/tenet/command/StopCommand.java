package tenet.command;

import tenet.command.Command;
import tenet.core.Simulator;

/**
 * A StopCommand can be schedule at any time to forcefully interrupt the
 * simulator. Some simulations can go on forever so this is quite useful.
 */
public class StopCommand extends Command {

	public StopCommand(double time) {
		super("Stop", time);
	}

	/**
	 * Just set the finished flag in the simulator.
	 */
	public void execute() {
		Simulator.getInstance().setFinished();
	}
}
