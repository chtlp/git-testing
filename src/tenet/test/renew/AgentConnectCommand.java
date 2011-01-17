package tenet.test.renew;

import tenet.command.Command;
import tenet.elem.ConnectionOrientedAgent;
import tenet.elem.ip.IPAddr;

public class AgentConnectCommand extends Command
{
  protected ConnectionOrientedAgent coagent;
  IPAddr src;
  IPAddr dst;
  int port;

  public AgentConnectCommand(double time, ConnectionOrientedAgent coagent, IPAddr src, IPAddr dst, int port)
  {
    super("AgentConnect", time);
    this.coagent = coagent;
    this.src = src;
    this.dst = dst;
    this.port = port;
  }

  public void execute()
  {
    this.coagent.connect(this.src, this.dst, this.port);
  }
}

/* Location:           D:\My Documents\eclipse\nachos-tenet\tester-0.2.jar
 * Qualified Name:     tenet.test.renew.AgentConnectCommand
 * JD-Core Version:    0.6.0
 */