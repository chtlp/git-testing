package tenet.firewall;

import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tenet.core.Log;
import tenet.droute.MyLib;
import tenet.elem.ip.IPAddr;
import tenet.elem.ip.IPv4Addr;
import tenet.elem.rule.IRuleBuilder;
import tenet.elem.rule.RuleFormatException;

public class RuleBuilder implements IRuleBuilder {

	@Override
	public MyRule build(String cmd) throws RuleFormatException {
		cmd = cmd.trim();
		String[] segs = cmd.split("\\W+");
		if (segs.length < 1)
			throw new RuleFormatException();
		else if (segs[0].matches("STCP"))
			return buildFilterRule(cmd);
		else if (segs[0].matches("NAT"))
			return buildNATRule(cmd);
		else if (segs[0].matches("II"))
			return buildIIRule(cmd);
		else
			throw new RuleFormatException();

	}

	static String ipPattern = "[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+";
	static String portPattern = "[0-9]+|ANY";

	static String iiPatternString = String
			.format("^II\\s+((SRC\\s+%1$s\\s+(%1$s+\\s+)?(%2$s))\\s+)?((DST\\s+%1$s\\s+(%1$s\\s+)?(%2$s))\\s+)?((([A-Z]+)\\s+)*)(DENY|ALLOW|\"([^\"]*)\")$",
					ipPattern, portPattern);
	static Pattern iiPattern = Pattern.compile(iiPatternString);

	private MyRule buildIIRule(String cmd) throws RuleFormatException {
		// System.out.println(cmd);
		IPAddr src_ip, dst_ip, src_mask, dst_mask;
		int src_port, dst_port;
		int flag = 0;
		int action;
		String content;
		Matcher matcher = iiPattern.matcher(cmd);
		if (matcher.find()) {
			try {
				String src = matcher.group(2);
				String dst = matcher.group(6);
				String flags = matcher.group(9);
				String actions = matcher.group(12);

				if (src != null) {
					String[] segs = src.split("\\s+");
					if (segs.length == 3) {
						src_ip = IPv4Addr.newInstance(segs[1]);
						src_mask = IPv4Addr.newInstance(0xFFFFFFFF);
						if (segs[2].matches("ANY"))
							src_port = MySTCPRule.ANY_PORT;
						else
							src_port = Integer.parseInt(segs[2]);
					}
					else if (segs.length == 4) {
						src_ip = IPv4Addr.newInstance(segs[1]);
						src_mask = IPv4Addr.newInstance(segs[2]);
						if (segs[3].matches("ANY"))
							src_port = MySTCPRule.ANY_PORT;
						else
							src_port = Integer.parseInt(segs[3]);
					}
					else
						throw new RuleFormatException();
				}
				else {
					src_ip = IPv4Addr.newInstance(0);
					src_mask = IPv4Addr.newInstance(0);
					src_port = MySTCPRule.ANY_PORT;
				}

				if (dst != null) {
					String[] segs = dst.split("\\s+");
					if (segs.length == 3) {
						dst_ip = IPv4Addr.newInstance(segs[1]);
						dst_mask = IPv4Addr.newInstance(0xFFFFFFFF);
						if (segs[2].matches("ANY"))
							dst_port = MySTCPRule.ANY_PORT;
						else
							dst_port = Integer.parseInt(segs[2]);
					}
					else if (segs.length == 4) {
						dst_ip = IPv4Addr.newInstance(segs[1]);
						dst_mask = IPv4Addr.newInstance(segs[2]);
						if (segs[3].matches("ANY"))
							dst_port = MySTCPRule.ANY_PORT;
						else
							dst_port = Integer.parseInt(segs[3]);
					}
					else
						throw new RuleFormatException();
				}
				else {
					dst_ip = IPv4Addr.newInstance(0);
					dst_mask = IPv4Addr.newInstance(0);
					dst_port = MySTCPRule.ANY_PORT;
				}

				if (flags != null) {
					String[] segs = MyLib.chopThree(flags.trim());
					for (String f : segs) {
						flag = flag | flagMap.get(f);
					}
				}

				action = actions.matches("ALLOW") ? MyIIRule.ACT_ALLOW
						: MyIIRule.ACT_RST;
				content = actions.matches("\".*\"") ? ".*"
						+ actions.substring(1, actions.length() - 1) + ".*"
						: ".*";

				return new MyIIRule(src_ip, src_mask, dst_ip, dst_mask, src_port,
						dst_port, flag, action, content);
			} catch (RuntimeException ex) {
				throw new RuleFormatException();
			}

		}
		throw new RuleFormatException();
	}

	static String natPatternString = String.format(
			"^NAT\\s+(%1$s)\\s+((%1$s)\\s+)?(%1$s)(\\s+[0-9]+|ALL)?$",
			ipPattern, portPattern);
	static Pattern natPattern = Pattern.compile(natPatternString);

	private MyRule buildNATRule(String cmd) throws RuleFormatException {
		IPv4Addr local_ip, local_mask, outside_ip;
		int protocol;
		Matcher matcher = natPattern.matcher(cmd);
		if (matcher.find()) {
			try {
				local_ip = IPv4Addr.newInstance(matcher.group(1));
				String m = matcher.group(3);
				local_mask = m == null ? IPv4Addr.newInstance(0xFFFFFFFF)
						: IPv4Addr.newInstance(m);
				outside_ip = IPv4Addr.newInstance(matcher.group(4));
				String p = matcher.group(5).trim();
				protocol = (p == null) || p.trim().matches("ALL") ? MyNATRule.ANY_PROTOCOL
						: Integer.parseInt(p);

				return new MyNATRule(local_ip, local_mask, outside_ip,
						IPv4Addr.newInstance(0xFFFFFFFF), protocol);
			} catch (RuntimeException ex) {
				throw new RuleFormatException();
			}
		}
		throw new RuleFormatException();
	}

	static String filterPatternString = String
			.format("^STCP\\s+((SRC\\s+%1$s+\\s+(%1$s+\\s+)?(%2$s))\\s+)?((DST\\s+%1$s+\\s+(%1$s+\\s+)?(%2$s))\\s+)?((([A-Z]+)\\s+)*)(DENY|ALLOW)$",
					ipPattern, portPattern);
	static Pattern filterPattern = Pattern.compile(filterPatternString);
	static Hashtable<String, Integer> flagMap = new Hashtable<String, Integer>();
	static {
		flagMap.put("CWR", MySTCPRule.CWR);
		flagMap.put("ECE", MySTCPRule.ECE);
		flagMap.put("URG", MySTCPRule.URG);
		flagMap.put("ACK", MySTCPRule.ACK);
		flagMap.put("PSH", MySTCPRule.PSH);
		flagMap.put("RST", MySTCPRule.RST);
		flagMap.put("SYN", MySTCPRule.SYN);
		flagMap.put("FIN", MySTCPRule.FIN);
	}

	private MyRule buildFilterRule(String cmd) throws RuleFormatException {
		IPAddr src_ip, dst_ip, src_mask, dst_mask;
		int src_port, dst_port;
		int flag = 0;
		int action;
		Matcher matcher = filterPattern.matcher(cmd);
		if (matcher.find()) {
			try {
				if (Log.testFlag('G')) {
					for (int i = 0; i <= matcher.groupCount(); ++i)
						Log.debug(i + " " + matcher.group(i));
				}
				String src = matcher.group(2);
				String dst = matcher.group(6);
				String flags = matcher.group(9);
				String actions = matcher.group(12);

				if (src != null) {
					String[] segs = src.split("\\s+");
					if (segs.length == 3) {
						src_ip = IPv4Addr.newInstance(segs[1]);
						src_mask = IPv4Addr.newInstance(0xFFFFFFFF);
						if (segs[2].matches("ANY"))
							src_port = MySTCPRule.ANY_PORT;
						else
							src_port = Integer.parseInt(segs[2]);
					}
					else if (segs.length == 4) {
						src_ip = IPv4Addr.newInstance(segs[1]);
						src_mask = IPv4Addr.newInstance(segs[2]);
						if (segs[3].matches("ANY"))
							src_port = MySTCPRule.ANY_PORT;
						else
							src_port = Integer.parseInt(segs[3]);
					}
					else
						throw new RuleFormatException();
				}
				else {
					src_ip = IPv4Addr.newInstance(0);
					src_mask = IPv4Addr.newInstance(0);
					src_port = MySTCPRule.ANY_PORT;
				}

				if (dst != null) {
					String[] segs = dst.split("\\s+");
					if (segs.length == 3) {
						dst_ip = IPv4Addr.newInstance(segs[1]);
						dst_mask = IPv4Addr.newInstance(0xFFFFFFFF);
						if (segs[2].matches("ANY"))
							dst_port = MySTCPRule.ANY_PORT;
						else
							dst_port = Integer.parseInt(segs[2]);
					}
					else if (segs.length == 4) {
						dst_ip = IPv4Addr.newInstance(segs[1]);
						dst_mask = IPv4Addr.newInstance(segs[2]);
						if (segs[3].matches("ANY"))
							dst_port = MySTCPRule.ANY_PORT;
						else
							dst_port = Integer.parseInt(segs[3]);
					}
					else
						throw new RuleFormatException();
				}
				else {
					dst_ip = IPv4Addr.newInstance(0);
					dst_mask = IPv4Addr.newInstance(0);
					dst_port = MySTCPRule.ANY_PORT;
				}

				if (flags != null) {
					String[] segs = MyLib.chopThree(flags.trim());
					for (String f : segs) {
						flag = flag | flagMap.get(f);
					}
				}

				action = actions.matches("ALLOW") ? MySTCPRule.ACT_ALLOW
						: MySTCPRule.ACT_DENY;

				return new MySTCPRule(src_ip, src_mask, dst_ip, dst_mask,
						src_port, dst_port, flag, action);
			} catch (RuntimeException ex) {
				throw new RuleFormatException();
			}

		}
		throw new RuleFormatException();
	}
}
