package tenet.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Log {
	private static final int LEV_DEBUG = 0;
	private static final int LEV_INFO = 1;
	private static final int LEV_WARN = 2;
	private static final int LEV_FATAL = 3;

	private static final int LEV_ALL = -1;
	private static final int LEV_OFF = 10;

	private static final String[] PREFIXES = { "DEBUG", "INFO", "WARN", "FATAL" };

	private static boolean[] debugFlags = new boolean[0x80];
	private static int debugLevel = LEV_ALL;

	public static void setupLog(String level, String flags) {
		if (level.toLowerCase().equals("info"))
			Log.debugLevel = LEV_INFO;
		else if (level.toLowerCase().equals("debug"))
			Log.debugLevel = LEV_DEBUG;
		else if (level.toLowerCase().equals("warning"))
			Log.debugLevel = LEV_WARN;
		else if (level.toLowerCase().equals("fatal"))
			Log.debugLevel = LEV_FATAL;
		else if (level.toLowerCase().equals("all"))
			Log.debugLevel = LEV_ALL;
		else
			Log.debugLevel = LEV_OFF;

		for (char c : flags.toCharArray())
			if (c >= 0 && c < 0x80)
				debugFlags[(int) c] = true;

		debug("Debug Level: " + level);
		debug("Debuf Flags: " + flags);
	}

	static PrintStream outFile;
	static {
		try {
			outFile = new PrintStream(new File("tenet.debug"));
		} catch (Exception ex) {
			System.err.println("debug file not open");
			System.exit(0);
		}
	}

	public static void debug(String msg) {
		if (debugLevel <= LEV_DEBUG)
			out(LEV_DEBUG, msg, System.out);
	}

	public static void info(String msg) {
		if (debugLevel <= LEV_INFO)
			out(LEV_INFO, msg, System.out);
	}

	public static void warn(String msg) {
		if (debugLevel <= LEV_WARN)
			out(LEV_WARN, msg, System.err);
	}

	public static void fatal(String msg) {
		if (debugLevel <= LEV_FATAL)
			out(LEV_FATAL, msg, System.err);
		System.exit(1);
	}

	public static boolean testFlag(char dbg) {
		return debugFlags[(int) dbg];
	}

	public static void debug(char dbg, String msg) {
		if (debugFlags[(int) dbg])
			out(LEV_DEBUG, msg, outFile);
	}

	private static void out(int lev, String msg, PrintStream p) {
		p.format("(%4.4f) [%s]", Simulator.getInstance().getTime(),
				PREFIXES[lev], msg);
		if (debugLevel <= LEV_DEBUG)
			p.print(" " + parseStackTrace());
		p.format(" - %s%n", msg);
	}

	private static String parseStackTrace() {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		Lib.assertTrue(elements.length > 4
				&& elements[3].getClassName().equals("tenet.core.Log"));
		return elements[4].toString();
	}
}
