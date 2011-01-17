package tenet.test.renew;

import tenet.command.Command;
import tenet.elem.ConnectionOrientedAgent;

public class AgentCloseCommand extends Command
{
  protected ConnectionOrientedAgent coagent;

  public AgentCloseCommand(double time, ConnectionOrientedAgent coagent)
  {
    super("AgentClose", time);
    this.coagent = coagent;
  }

  public void execute()
  {
    this.coagent.disconnect();
  }
}

/* Location:           D:\My Documents\eclipse\nachos-tenet\tester-0.2.jar
 * Qualified Name:     tenet.test.renew.AgentCloseCommand
 * JD-Core Version:    0.6.0
 */