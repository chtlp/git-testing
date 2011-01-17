package tenet.test.builder;

/**
 * action of terminating the simulation
 * 
 * @author TLP
 * 
 */
public class TerminateAction extends Action {
	double time;

	public TerminateAction(double time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return "terminate " + time;
	}

}
