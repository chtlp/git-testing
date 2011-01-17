package tenet.test.renew;

import tenet.command.Command;
import tenet.elem.ConnectionOrientedAgent;

public class AgentSendCommand extends Command
{
  protected ConnectionOrientedAgent coagent;
  protected String content;

  public AgentSendCommand(double time, ConnectionOrientedAgent coagent, String content)
  {
    super("AgentSend", time);
    this.coagent = coagent;
    this.content = content;
  }

  public void execute()
  {
    this.coagent.send(this.content.getBytes().length, this.content.getBytes(), 0);
  }
}

/* Location:           D:\My Documents\eclipse\nachos-tenet\tester-0.2.jar
 * Qualified Name:     tenet.test.renew.AgentSendCommand
 * JD-Core Version:    0.6.0
 */