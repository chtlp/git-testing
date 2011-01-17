package tenet.test.renew;

import java.util.TreeMap;
import java.util.TreeSet;

public class Environment
{
  private static Environment instance = null;
  protected TreeMap<String, ?> envMap;

  public static Environment getInstance()
  {
    if (instance == null) instance = new Environment();
    return instance;
  }

  protected <U> void addToSet(String title, U content)
  {
    TreeSet set = (TreeSet)this.envMap.get(title);
    if (!this.envMap.containsKey(title))
      set = new TreeSet();
    set.add(content);
  }

  protected <U> void removeFromSet(String title, U content)
  {
    TreeSet set = (TreeSet)this.envMap.get(title);
    if (!this.envMap.containsKey(title))
      set = new TreeSet();
    set.remove(content);
  }

  protected <U> boolean containInSet(String title, U content)
  {
    TreeSet set = (TreeSet)this.envMap.get(title);
    if (!this.envMap.containsKey(title))
      set = new TreeSet();
    return set.contains(content);
  }
}

/* Location:           D:\My Documents\eclipse\nachos-tenet\tester-0.2.jar
 * Qualified Name:     tenet.test.renew.Environment
 * JD-Core Version:    0.6.0
 */