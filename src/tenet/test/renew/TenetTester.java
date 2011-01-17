package tenet.test.renew;

import java.io.File;
import java.io.PrintStream;
import tenet.core.Simulator;

public class TenetTester
{
  private static TestProcessor tp;

  public static void main(String[] args)
  {
    Simulator.getInstance();
    tp = new TestProcessor();
    if (args.length < 2)
    {
      System.err.println("usage:\tTenetTester <option> <task-file1> [... <task-filen>]");
      System.exit(1);
    }
    for (int i = 1; i < args.length; i++)
    {
      File f = new File(args[i]);
      if ((!f.exists()) || (f.isDirectory()))
      {
        System.err.println(args[i] + " is not a file or doesn't exist");
        System.exit(1);
      }
      tp.loadFile(f);
    }
    if (args[0].contains("d"))
    {
      System.out.println();
    }
    if (args[0].contains("r"))
    {
      tp.run();
    }
  }
}

/* Location:           D:\My Documents\eclipse\nachos-tenet\tester-0.2.jar
 * Qualified Name:     tenet.test.renew.TenetTester
 * JD-Core Version:    0.6.0
 */