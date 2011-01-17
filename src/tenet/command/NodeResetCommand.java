package tenet.command;

import tenet.elem.phys.Node;

public class NodeResetCommand extends Command {

	private Node m_node;

	public NodeResetCommand(Node n, double time) {
		super("reset", time);
		m_node = n;
	}

	@Override
	public void execute() {
		m_node.reset();
	}
}
