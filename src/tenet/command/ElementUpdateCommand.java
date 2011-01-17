package tenet.command;

import tenet.elem.Element;

/**
 * ElementUpdateCommand is a command class that can be used genericly to
 * schedule a call to any element's update() function at a specific time.
 */
public class ElementUpdateCommand extends Command {

	private Element m_element;

	public ElementUpdateCommand(Element element, double time) {
		super("ElementUpdate", time);
		m_element = element;
	}

	/**
	 * Call the update function of the element.
	 */
	public void execute() {
		m_element.update();
	}

}
