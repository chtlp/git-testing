package tenet.test.renew;

import tenet.command.Command;
import tenet.elem.ConnectionOrientedAgent;
import tenet.elem.ip.IPAddr;

public class AgentListenCommand extends Command
{
  protected ConnectionOrientedAgent coagent;
  IPAddr src;

  public AgentListenCommand(double time, ConnectionOrientedAgent coagent, IPAddr src)
  {
    super("AgentListen", time);
    this.coagent = coagent;
    this.src = src;
  }

  public void execute()
  {
    this.coagent.listen(this.src);
  }
}

/* Location:           D:\My Documents\eclipse\nachos-tenet\tester-0.2.jar
 * Qualified Name:     tenet.test.renew.AgentListenCommand
 * JD-Core Version:    0.6.0
 */