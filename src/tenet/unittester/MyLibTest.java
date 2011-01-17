package tenet.unittester;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import junit.framework.*;
import static org.junit.Assert.*;
import org.junit.Test;

import tenet.droute.MyLib;

public class MyLibTest {

	@Test
	public void testcpbfhp() {
		assertEquals(MyLib.cpbfhp(0xFFFFFFFF), 32);
		assertEquals(MyLib.cpbfhp(0xFFFF0000), 16);
	}

	@Test
	public void testSeqCmp() {
		assertEquals(
				MyLib.seqCmp(Integer.MAX_VALUE - 3, Integer.MIN_VALUE + 3), 7);
		assertEquals(MyLib.seqCmp(7, 100), 93);
	}

	@Test
	public void testEquals() {
		assertTrue(7000 == (Integer) 7000);
	}

	@Test
	public void testIterator() {
		LinkedList<Integer> list = new LinkedList<Integer>();
		for (int i = 0; i < 10; ++i)
			list.add(i);
		for (Iterator<Integer> iter = list.iterator(); iter.hasNext();) {
			if (iter.next() % 2 == 1)
				iter.remove();
		}
		System.out.println(list);
	}

	@Test
	public void testIterator2() {
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		map.put(1, "one");
		map.put(2, "two");
		Iterator<Integer> iter1 = map.keySet().iterator();
		iter1.next();
		iter1.remove();
		System.out.println(map);
		Iterator<String> iter2 = map.values().iterator();
		iter2.next();
		iter2.remove();
		System.out.println(map);
	}
}
