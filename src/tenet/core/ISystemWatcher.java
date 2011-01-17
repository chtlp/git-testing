/**
 * 
 */
package tenet.core;

import tenet.command.Command;

/**
 * @author Meilun Sheng
 * @version 2010-12-14 обнГ04:07:16
 */
public interface ISystemWatcher {
	public void eventRecord(Object obj, StackTraceElement[] stackTraceElements);
}
