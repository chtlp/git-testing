package tenet.test.renew;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Stack;
import tenet.command.StopCommand;
import tenet.constant.Protocols;
import tenet.core.Simulator;
import tenet.elem.ConnectionLessAgent;
import tenet.elem.ConnectionOrientedAgent;
import tenet.elem.IPacketWatcher;
import tenet.elem.IPacketWatcherHolder;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPv4Addr;
import tenet.elem.phys.Interface;
import tenet.elem.phys.Link;
import tenet.elem.phys.Node;
import tenet.elem.rule.IRuleBuilder;
import tenet.elem.rule.IRuleExecutable;
import tenet.elem.rule.Rule;
import tenet.elem.rule.RuleFormatException;

public class TestProcessor {
	protected HashMap<String, Node> nodeMap;
	protected HashMap<String, Interface> ifaceMap;
	protected HashMap<String, Class<?>> classMap;
	protected HashMap<String, HashMap<Integer, ConnectionLessAgent>> tlhMap;
	protected HashMap<String, HashMap<Integer, HashMap<Integer, ConnectionOrientedAgent>>> agentMap;
	protected HashMap<Class<? extends ConnectionLessAgent>, Class<? extends ConnectionOrientedAgent>> coMap;
	protected IRuleBuilder rulebuilder;
	protected HashMap<String, String> marcosSet;
	private static final int REGISTRY_CLASS = 0;
	private static final int SETUP_NODE = 1;
	private static final int SETUP_IFACE = 2;
	private static final int SETUP_LINK = 3;
	private static final int ROUTE_ADD = 4;
	private static final int ROUTE_DEL = 5;
	private static final int RULE_ADD = 6;
	private static final int SETUP_TPL = 7;
	private static final int AGENT_LISTEN = 8;
	private static final int SETUP_AGENT = 9;
	private static final int AGENT_CONNECT = 10;
	private static final int AGENT_SEND = 11;
	private static final int SETUP_WATCHER = 12;
	private static final int AGENT_CLOSE = 13;
	private static final int SETUP_TPLBIND = 14;
	private static final int IMPORT_FILE = 15;
	private static final int DEFINE_MARCO = 16;
	private static final int UNDEFINE_MARCO = 17;
	private static final int IF_MARCODEFINED = 18;
	private static final int IF_MARCOUNDEFINED = 19;
	private static final int IF_END = 20;
	private static final int TERMINATE = 21;
	private Stack<Boolean> execute_stack = new Stack();
	private boolean isExecute = true;

	public TestProcessor() {
		this.execute_stack.push(Boolean.valueOf(true));
		this.marcosSet = new HashMap();
		this.nodeMap = new HashMap();
		this.ifaceMap = new HashMap();
		this.classMap = new HashMap();
		this.tlhMap = new HashMap();
		this.agentMap = new HashMap();
		this.coMap = new HashMap();
		this.rulebuilder = null;
		this.classMap.put("ip", IPv4Addr.class);
	}

	protected void addRoute(String name, String targetIP, String targetmask,
			String InterfaceIP, Integer metric) {
		Node node = (Node) this.nodeMap.get(name);
		if (node == null) {
			System.err.println("Node:" + name + " is used before setup");
			System.exit(1);
		}
		IPAddr tgrIP = getIP(targetIP);
		IPAddr tgrMask = getIP(targetmask);
		Interface iface = (Interface) this.ifaceMap.get(InterfaceIP);
		node.addRoute(tgrIP, tgrMask, iface, metric);
	}

	private Rule buildRule(String rule) {
		IRuleBuilder builder = getRuleBuilder();
		try {
			return builder.build(rule);
		} catch (RuleFormatException e) {
			System.err.println("Failed to Build Rule:" + rule);
			System.exit(1);
		}
		return null;
	}

	protected <U> Constructor<? extends U> constructor(Class<U> sampleU,
			Class<? extends U> cls, Class<?>[] clslist) {
		try {
			Constructor constructor = cls.getConstructor(clslist);
			return constructor;
		} catch (SecurityException e) {
			System.err.println("Security Exception: while get Constructor "
					+ cls.getName() + "(" + getClassListString(clslist) + ")");
			System.exit(1);
		} catch (NoSuchMethodException e) {
			System.err.println("Cannot find Constructor " + cls.getName() + "("
					+ getClassListString(clslist) + ")");
			System.exit(1);
		}
		return null;
	}

	protected <U> U create(Class<U> sampleU,
			Constructor<? extends U> constructor, Object[] objlist) {
		try {
			return constructor.newInstance(objlist);
		} catch (IllegalArgumentException e) {
			System.err.println(constructor.toString()
					+ " is called with mistake parameters.");
			System.exit(1);
		} catch (InstantiationException e) {
			System.err.println(constructor.getName()
					+ " is abstract, can't be created");
			System.exit(1);
		} catch (IllegalAccessException e) {
			System.err.println(constructor.toString() + " is not public");
			System.exit(1);
		} catch (InvocationTargetException e) {
			System.err.println(constructor.toString() + " met an Exception:\n"
					+ e.getTargetException().toString());
			System.err.println("Error Call Stack:");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		return null;
	}

	private ConnectionLessAgent getCLAgent(String name, Integer protocol) {
		HashMap tlonNMap = (HashMap) this.tlhMap.get(name);
		if (tlonNMap == null) {
			System.err.println("No Transport layer Protocol on Node " + name
					+ " registried");
			System.exit(1);
		}
		ConnectionLessAgent clagent = (ConnectionLessAgent) tlonNMap
				.get(protocol);
		if (clagent == null) {
			System.err.println("No Transport layer Protocol:" + protocol
					+ " on Node " + name + " registried");
			System.exit(1);
		}
		return clagent;
	}

	private String getClassListString(Class<?>[] clslist) {
		boolean flag = false;
		StringBuilder sb = new StringBuilder();
		for (Class _cls : clslist) {
			if (flag)
				sb.append(',');
			sb.append(_cls.getName());
			flag = true;
		}
		return sb.toString();
	}

	private int getCommandType(String head) throws TestProcessor.ParseException {
		head = head.trim().toLowerCase();
		if (head.equals("regclass"))
			return 0;
		if (head.equals("node")) {
			return 1;
		}

		if (head.equals("iface")) {
			return 2;
		}
		if (head.equals("link")) {
			return 3;
		}

		if (head.equals("addroute")) {
			return 4;
		}
		if (head.equals("delroute"))
			return 5;
		if (head.equals("addrule"))
			return 6;
		if (head.equals("tplayer")) {
			return 7;
		}

		if (head.equals("bindtpl"))
			return 14;
		if (head.equals("newagent")) {
			return 9;
		}

		if (head.equals("listen"))
			return 8;
		if (head.equals("connect")) {
			return 10;
		}
		if (head.equals("send"))
			return 11;
		if (head.equals("close")) {
			return 13;
		}
		if (head.equals("watcher")) {
			return 12;
		}
		if (head.equals("import"))
			return 15;
		if (head.equals("define"))
			return 16;
		if (head.equals("undefine"))
			return 17;
		if (head.equals("ifdef"))
			return 18;
		if (head.equals("ifundef"))
			return 19;
		if (head.equals("endif"))
			return 20;
		if (head.equals("terminate"))
			return 21;
		throw new ParseException();
	}

	private IPAddr getIP(String IP) {
		Class clsIP = getType(IPAddr.class, "ip");
		try {
			Method newIP = clsIP.getMethod("newInstance",
					new Class[] { String.class });
			return (IPAddr) newIP.invoke(null, new Object[] { IP });
		} catch (SecurityException e) {
			System.err.println("Type:" + clsIP.getName()
					+ " is rejected to access for security.");
			System.exit(1);
		} catch (NoSuchMethodException e) {
			System.err.println("Method:" + clsIP.getName()
					+ ".newInstance(String) doesn't exist.");
			System.exit(1);
		} catch (IllegalArgumentException e) {
			System.err.println("Method:" + clsIP.getName()
					+ ".newInstance(String) hasn't been implemented.");
			System.exit(1);
		} catch (IllegalAccessException e) {
			System.err.println("Method:" + clsIP.getName()
					+ ".newInstance(String) isn't public.");
			System.exit(1);
		} catch (InvocationTargetException e) {
			System.err.println("Method:" + clsIP.getName()
					+ ".newInstance(String) throw an Exception:\n"
					+ e.getTargetException().toString());
			System.exit(1);
		}
		return null;
	}

	private Integer getProtocol(String string) {
		Integer protocol = null;
		try {
			protocol = Integer.valueOf(Integer.parseInt(string));
		} catch (NumberFormatException e) {
			Class clsptl = Protocols.class;
			try {
				Field f = clsptl.getField(string);
				protocol = Integer.valueOf(f.getInt(new Protocols()));
			} catch (SecurityException e1) {
				System.err
						.println("Security Exception: on access Field:Protocols."
								+ string);
				System.exit(1);
			} catch (NoSuchFieldException e1) {
				System.err.println("Field:Protocols." + string
						+ " doesn't exist");
				System.exit(1);
			} catch (IllegalArgumentException e1) {
				System.err.println("Protocols() can't be accessed");
				System.exit(1);
			} catch (IllegalAccessException e1) {
				System.err.println("Protocols() can't be accessed");
				System.exit(1);
			}
		}
		return protocol;
	}

	private IRuleBuilder getRuleBuilder() {
		if (this.rulebuilder == null) {
			IRuleBuilder builder = (IRuleBuilder) create(
					IRuleBuilder.class,
					constructor(IRuleBuilder.class,
							getType(IRuleBuilder.class, "rulebuilder"),
							new Class[0]), new Object[0]);

			this.rulebuilder = builder;
		}
		return this.rulebuilder;
	}

	protected <U> Class<? extends U> getType(Class<U> sampleU, String type) {
		if (this.classMap.containsKey(type.toLowerCase().trim())) {
			Class cls = (Class) this.classMap.get(type.toLowerCase().trim());
			try {
				cls.asSubclass(sampleU);
				return cls;
			} catch (ClassCastException e) {
				System.err
						.println(cls.getName() + " is no a sub-class of Node");
				System.exit(1);
			}
		}
		else {
			System.err.println("Type:" + type + " is used before registered");
			System.exit(1);
		}
		return null;
	}

	public void loadFile(File f) {
		try {
			LineNumberReader lnr = new LineNumberReader(new FileReader(f));

			int lineno = 0;
			String s;
			while ((s = lnr.readLine()) != null) {
				lineno++;
				parse(s.trim(), lineno);
			}
			lnr.close();
		} catch (FileNotFoundException e) {
			System.err.println(f.getName() + " is not a file or doesn't exist");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("I/O Error while reading" + f.getName());
			System.exit(1);
		}
	}

	protected void parse(String line, int lineno) {
		this.isExecute = ((Boolean) this.execute_stack.peek()).booleanValue();

		String[] linec = line.split("#");
		if ((linec.length < 1) || (linec[0].length() == 0)
				|| (linec[0].trim().length() == 0))
			return;
		String[] cmds = linec[0].split("\\s");
		try {
			switch (getCommandType(cmds[0])) {
			case 0:
				if (!this.isExecute)
					return;
				registryClass(cmds[1], cmds[2]);
				break;
			case 1:
				if (!this.isExecute)
					return;
				setupNode(cmds[1], cmds[2]);
				break;
			case 2:
				if (!this.isExecute)
					return;
				try {
					setupInterface(cmds[1], cmds[2],
							Integer.valueOf(Integer.parseInt(cmds[3])));
				} catch (NumberFormatException e) {
					System.out.println("Bandwith field is not a number on #"
							+ lineno + ":\n" + line);
					System.exit(1);
				}
				break;
			case 3:
				if (!this.isExecute)
					return;
				try {
					setupLink(cmds[1], cmds[2],
							Integer.valueOf(Integer.parseInt(cmds[3])),
							Double.valueOf(Double.parseDouble(cmds[4])),
							Double.valueOf(Double.parseDouble(cmds[5])));
				} catch (NumberFormatException e) {
					System.out
							.println("Bandwith/Delay/Error_rate field is not an acceptable number on #"
									+ lineno + ":\n" + line);
					System.exit(1);
				}
				break;
			case 4:
				if (!this.isExecute)
					return;
				try {
					addRoute(cmds[1], cmds[2], cmds[3], cmds[4],
							Integer.valueOf(Integer.parseInt(cmds[5])));
				} catch (NumberFormatException e) {
					System.out
							.println("metric field is not an acceptable number on #"
									+ lineno + ":\n" + line);
					System.exit(1);
				}
				break;
			case 5:
				if (!this.isExecute)
					return;
				try {
					removeRoute(cmds[1], cmds[2]);
				} catch (NumberFormatException e) {
					System.out
							.println("metric field is not an acceptable number on #"
									+ lineno + ":\n" + line);
					System.exit(1);
				}
				break;
			case 6:
				if (!this.isExecute)
					return;
				StringBuilder rule = new StringBuilder();
				for (int i = 2; i < cmds.length; i++) {
					if (i != 2)
						rule.append(' ');
					rule.append(cmds[i]);
				}
				setupRule(cmds[1], rule.toString());
				break;
			case 7:
				if (!this.isExecute)
					return;
				Integer protocol = getProtocol(cmds[3]);
				setupTransportLayer(cmds[1], cmds[2], protocol);
				break;
			case 9:
				if (!this.isExecute)
					return;
				Integer protocol1 = getProtocol(cmds[3]);
				try {
					setupAgent(cmds[1], cmds[2], protocol1,
							Integer.parseInt(cmds[4]));
				} catch (NumberFormatException e) {
					System.out
							.println("Port field is not an acceptable number on #"
									+ lineno + ":\n" + line);
					System.exit(1);
				}
				break;
			case 14:
				if (!this.isExecute)
					return;
				setupTPLBind(cmds[1], cmds[2]);
				break;
			case 12:
				if (!this.isExecute)
					return;
				if (cmds.length < 4) {
					setupWatcher(cmds[1], cmds[2], Integer.valueOf(-1));
				}
				else {
					Integer protocol2 = getProtocol(cmds[3]);
					setupWatcher(cmds[1], cmds[2], protocol2);
				}
				break;
			case 8:
				if (!this.isExecute)
					return;
				Integer protocol5 = getProtocol(cmds[2]);
				try {
					ConnectionOrientedAgent coagent = getCOAgent(cmds[1],
							protocol5,
							Integer.valueOf(Integer.parseInt(cmds[3])));
					IPAddr local_ip = getIP(cmds[4]);
					Simulator.getInstance().schedule(
							new AgentListenCommand(Double.parseDouble(cmds[5]),
									coagent, local_ip));
				} catch (NumberFormatException e) {
					System.out
							.println("Port/Time field is not an acceptable number on #"
									+ lineno + ":\n" + line);
					System.exit(1);
				}
				break;
			case 10:
				if (!this.isExecute)
					return;
				Integer protocol6 = getProtocol(cmds[2]);
				try {
					ConnectionOrientedAgent coagent = getCOAgent(cmds[1],
							protocol6,
							Integer.valueOf(Integer.parseInt(cmds[3])));
					IPAddr src_ip = getIP(cmds[4]);
					IPAddr dst_ip = getIP(cmds[5]);
					Integer dst_port = Integer.valueOf(Integer
							.parseInt(cmds[6]));
					Simulator.getInstance().schedule(
							new AgentConnectCommand(
									Double.parseDouble(cmds[7]), coagent,
									src_ip, dst_ip, dst_port.intValue()));
				} catch (NumberFormatException e) {
					System.out
							.println("Port/Time field is not an acceptable number on #"
									+ lineno + ":\n" + line);
					System.exit(1);
				}
				break;
			case 11:
				if (!this.isExecute)
					return;
				Integer protocol7 = getProtocol(cmds[2]);
				try {
					ConnectionOrientedAgent coagent = getCOAgent(cmds[1],
							protocol7,
							Integer.valueOf(Integer.parseInt(cmds[3])));
					StringBuilder sb = new StringBuilder();
					for (int i = 5; i < cmds.length; i++) {
						if (i != 5)
							sb.append(' ');
						sb.append(cmds[i]);
					}
					Simulator.getInstance().schedule(
							new AgentSendCommand(Double.parseDouble(cmds[4]),
									coagent, sb.toString()));
				} catch (NumberFormatException e) {
					System.out
							.println("Port/Time field is not an acceptable number on #"
									+ lineno + ":\n" + line);
					System.exit(1);
				}
				break;
			case 13:
				if (!this.isExecute)
					return;
				Integer protocol8 = getProtocol(cmds[2]);
				try {
					ConnectionOrientedAgent coagent = getCOAgent(cmds[1],
							protocol8,
							Integer.valueOf(Integer.parseInt(cmds[3])));
					Simulator.getInstance().schedule(
							new AgentCloseCommand(Double.parseDouble(cmds[4]),
									coagent));
				} catch (NumberFormatException e) {
					System.out
							.println("Port/Time field is not an acceptable number on #"
									+ lineno + ":\n" + line);
					System.exit(1);
				}
				break;
			case 15:
				if (!this.isExecute)
					return;
				// System.out.println("Import " + cmds[1]);
				loadFile(new File(cmds[1]));
				break;
			case 16:
				if (!this.isExecute)
					return;
				if (cmds.length == 2)
					this.marcosSet.put(cmds[1], "");
				else
					this.marcosSet.put(cmds[1], cmds[2]);
				break;
			case 17:
				if (!this.isExecute)
					return;
				this.marcosSet.remove(cmds[1]);
				break;
			case 18:
				Boolean condition = Boolean
						.valueOf((((Boolean) this.execute_stack.peek())
								.booleanValue())
								&& (this.marcosSet.containsKey(cmds[1])));
				this.execute_stack.push(condition);
				this.isExecute = condition.booleanValue();
				break;
			case 19:
				Boolean condition2 = Boolean
						.valueOf((((Boolean) this.execute_stack.peek())
								.booleanValue())
								&& (!this.marcosSet.containsKey(cmds[1])));
				this.execute_stack.push(condition2);
				this.isExecute = condition2.booleanValue();
				break;
			case 20:
				this.isExecute = ((Boolean) this.execute_stack.pop())
						.booleanValue();
				break;
			case 21:
				try {
					Simulator.getInstance().schedule(
							new StopCommand(Double.parseDouble(cmds[1])));
				} catch (NumberFormatException e) {
					System.out
							.println("Time field is not an acceptable number on #"
									+ lineno + ":\n" + line);
					System.exit(1);
				}
			}
		} catch (ParseException e) {
			System.err.println("Task File Parse error on #" + lineno + ":\n"
					+ line);
			e.printStackTrace();
			System.exit(1);
		} catch (NullPointerException e) {
			System.err.println("Argument missed on #" + lineno + ":\n" + line);
			System.exit(1);
		}
	}

	protected ConnectionOrientedAgent getCOAgent(String node, Integer protocol,
			Integer port) {
		HashMap protocolMap = (HashMap) this.agentMap.get(node);
		if (protocolMap == null) {
			System.err.println("Agent on" + node + ":" + protocol + ":" + port
					+ " is used before setup");
			System.exit(1);
		}
		HashMap portMap = (HashMap) protocolMap.get(protocol);
		if (portMap == null) {
			System.err.println("Agent on" + node + ":" + protocol + ":" + port
					+ " is used before setup");
			System.exit(1);
		}
		ConnectionOrientedAgent ret = (ConnectionOrientedAgent) portMap
				.get(port);
		if (ret == null) {
			System.err.println("Agent on" + node + ":" + protocol + ":" + port
					+ " is used before setup");
			System.exit(1);
		}
		return ret;
	}

	protected void registryClass(String name, String type) {
		try {
			this.classMap.put(name.toLowerCase().trim(), Class.forName(type));
		} catch (ClassNotFoundException e) {
			System.err.println(type + " can't be found. Check CLASSPATH.");
			System.exit(1);
		}
	}

	protected void removeRoute(String name, String targetIP) {
		Node node = (Node) this.nodeMap.get(name);
		if (node == null) {
			System.err.println("Node:" + name + " is used before setup");
			System.exit(1);
		}
		IPAddr tgrIP = getIP(targetIP);
		node.deleteRoute(tgrIP);
	}

	protected void setupAgent(String type, String name, Integer protocol,
			int port) {
		ConnectionLessAgent clagent = getCLAgent(name, protocol);
		Class clsCO = (Class) this.coMap.get(clagent.getClass());
		ConnectionOrientedAgent coagent = (ConnectionOrientedAgent) create(
				ConnectionOrientedAgent.class,
				constructor(ConnectionOrientedAgent.class, clsCO,
						new Class[] { Integer.TYPE }),
				new Object[] { Integer.valueOf(port) });
		ConnectionOrientedAgent agent = (ConnectionOrientedAgent) create(
				ConnectionOrientedAgent.class,
				constructor(ConnectionOrientedAgent.class,
						getType(ConnectionOrientedAgent.class, type),
						new Class[] { Integer.TYPE }),
				new Object[] { Integer.valueOf(0) });
		clagent.attach(coagent, port);
		coagent.attach(agent, 0);
		HashMap map1 = (HashMap) this.agentMap.get(name);
		if (map1 == null)
			map1 = new HashMap();
		HashMap map2 = (HashMap) map1.get(protocol);
		if (map2 == null)
			map2 = new HashMap();
		map2.put(Integer.valueOf(port), agent);
		map1.put(protocol, map2);
		this.agentMap.put(name, map1);
	}

	protected void setupInterface(String name, String IP, Integer bandwidth) {
		IPAddr ip = getIP(IP);
		Interface iface = (Interface) create(
				Interface.class,
				constructor(Interface.class, getType(Interface.class, "iface"),
						new Class[] { IPAddr.class, Integer.TYPE }),
				new Object[] { ip, Integer.valueOf(bandwidth.intValue()) });
		Node node = (Node) this.nodeMap.get(name);
		if (node == null) {
			System.err.println("Node:" + name + " is used before setup");
			System.exit(1);
		}
		node.attach(iface);
		this.ifaceMap.put(IP, iface);
	}

	protected void setupLink(String IP1, String IP2, Integer bandwidth,
			Double delay, Double error) {
		IPAddr ip1 = getIP(IP1);
		IPAddr ip2 = getIP(IP2);
		Interface iface1 = (Interface) this.ifaceMap.get(IP1);
		Interface iface2 = (Interface) this.ifaceMap.get(IP2);
		if ((iface1 == null) || (iface2 == null)) {
			System.err.println("IFace:" + (iface1 == null ? IP1 : IP2)
					+ " is used before setup");
			System.exit(1);
		}
		Link link = (Link) create(
				Link.class,
				constructor(Link.class, getType(Link.class, "link"),
						new Class[] { Integer.TYPE, Double.TYPE, Double.TYPE }),
				new Object[] { bandwidth, delay, error });
		iface1.attach(link, true);
		iface2.attach(link, true);
	}

	protected void setupNode(String typename, String name) {
		// if (typename.equals("nodes")) {
		// System.out.println("gota");
		// }
		Node node = (Node) create(
				Node.class,
				constructor(Node.class, getType(Node.class, typename),
						new Class[] { String.class }), new Object[] { name });
		this.nodeMap.put(name, node);
	}

	protected void setupRule(String name, String rule) {
		Node node = (Node) this.nodeMap.get(name);
		if (node == null) {
			System.err.println("Node:" + name + " is used before setup");
			System.exit(1);
		}
		if (!(node instanceof IRuleExecutable)) {
			System.err
					.println("Node:"
							+ name
							+ " is asked to setup a rule, but it is not IRuleExecutable.");
			System.exit(1);
		}
		IRuleExecutable re = (IRuleExecutable) node;
		Rule frule = buildRule(rule);
		re.addRule(frule);
	}

	protected void setupTPLBind(String clagent, String coagent) {
		Class clsCO = getType(ConnectionOrientedAgent.class, coagent);
		Class clsCL = getType(ConnectionLessAgent.class, clagent);
		this.coMap.put(clsCL, clsCO);
	}

	protected void setupTransportLayer(String type, String name,
			Integer protocol) {
		ConnectionLessAgent clagent = (ConnectionLessAgent) create(
				ConnectionLessAgent.class,
				constructor(ConnectionLessAgent.class,
						getType(ConnectionLessAgent.class, type), new Class[0]),
				new Object[0]);

		Node node = (Node) this.nodeMap.get(name);
		if (node == null) {
			System.err.println("Node:" + name + " is used before setup");
			System.exit(1);
		}
		node.attach(clagent, protocol.intValue());
		HashMap protocolMap = (HashMap) this.tlhMap.get(name);
		if (protocolMap == null)
			protocolMap = new HashMap();
		protocolMap.put(protocol, clagent);
		this.tlhMap.put(name, protocolMap);
	}

	protected void setupWatcher(String type, String name, Integer protocol) {
		Node node = (Node) this.nodeMap.get(name);
		if (node == null) {
			System.err.println("Node:" + name + " is used before setup");
			System.exit(1);
		}
		if (!(node instanceof IPacketWatcherHolder)) {
			System.err.println("Node:" + name
					+ " is no an implement of IPacketWatcherHolder");
			System.exit(1);
		}
		IPacketWatcherHolder pwh = (IPacketWatcherHolder) node;
		IPacketWatcher watcher = (IPacketWatcher) create(
				IPacketWatcher.class,
				constructor(IPacketWatcher.class,
						getType(IPacketWatcher.class, type), new Class[0]),
				new Object[0]);

		pwh.setPacketWatcher(watcher, protocol);
	}

	public void run() {
		Simulator.getInstance().run();
	}

	class ParseException extends Exception {
		private static final long serialVersionUID = -3829480681483413243L;

		ParseException() {
		}
	}
}

/*
 * Location: D:\My Documents\eclipse\nachos-tenet\tester-0.2.jar Qualified Name:
 * tenet.test.renew.TestProcessor JD-Core Version: 0.6.0
 */