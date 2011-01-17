package tenet.test.builder;

/**
 * type of the nodes, different types of nodes will be registered to different
 * classes
 * 
 * @author TLP
 * 
 */
public enum NodeType {
	Host("node"), Router("router"), Firewall("firewall"), Switch("switch");

	private String des;

	private NodeType(String des) {
		this.des = des;
	}

	@Override
	public String toString() {
		return des;
	}

}
