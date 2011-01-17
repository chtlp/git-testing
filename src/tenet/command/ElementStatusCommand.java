package tenet.command;

import tenet.elem.Element;

public class ElementStatusCommand extends Command {

	private Element m_element;
	private int m_status;

	public ElementStatusCommand(Element element, int status, double time) {
		super("Element Status Command", time);
		m_element = element;
		m_status = status;
	}

	public void execute() {
		m_element.setStatus(m_status);
	}
}
