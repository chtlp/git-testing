package tenet.droute;

public class Node2 extends DRNode {

	public Node2() {
		super();
	}

	public Node2(String name) {
		super(name);
	}

	@Override
	protected boolean supportRouting() {
		return false;
	}

}
