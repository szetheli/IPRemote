/**
 * 
 */
package svenz.remote.common.utilities;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

/**
 * @author Sven Zethelius
 *
 */
public class BeanAdapterUnitTests
{
	private final BeanAdapter m_adapter = new BeanAdapter();

	@Test
	public void testRoot() throws Exception
	{
		Object o = new Object();
		m_adapter.addRoot("a", o);

		assertEquals(o, m_adapter.resolve(Collections.singletonList("a")));
	}

	@Test
	public void testNest1() throws Exception
	{
		Container c1 = new Container(null, "O2sss"),
			c2 = new Container(c1, "asf");
		m_adapter.addRoot("2", c2);
		
		assertEquals(c1, m_adapter.resolve(Arrays.asList("2", "o1")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoRoot() throws Exception
	{
		Object o = new Object();
		m_adapter.addRoot("a", o);
		m_adapter.resolve(Arrays.asList("notFound"));
	}

	@Test(expected = NoSuchMethodException.class)
	public void testPropertyNotFound() throws Exception
	{
		Object o = new Object();
		m_adapter.addRoot("a", o);
		m_adapter.resolve(Arrays.asList("a", "foo"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullValue() throws Exception
	{
		Container c1 = new Container(null, "O2sss");
		m_adapter.addRoot("1", c1);
		assertEquals(c1, m_adapter.resolve(Arrays.asList("1", "o1", "notFound")));
	}

	@Test
	public void testCached() throws Exception
	{
		Container c1 = new Container(null, "O2sss"),
				c2 = new Container(c1, "asf");
		m_adapter.addRoot("2", c2);
			
		assertEquals("O2sss", m_adapter.resolve(Arrays.asList("2", "o1", "o2")));
		assertEquals("asf", m_adapter.resolve(Arrays.asList("2", "o2")));

	}

	@Test
	public void testIsMethod() throws Exception
	{
		Container c1 = new Container(null, "O2sss"), 
				c2 = new Container(c1, "asf");
		m_adapter.addRoot("2", c2);
		assertEquals(true, m_adapter.resolve(Arrays.asList("2", "bool")));
		assertEquals(false, m_adapter.resolve(Arrays.asList("2", "o1", "bool")));
	}

	@SuppressWarnings("unused")
	private static class Container
	{
		private final Object m_o1;
		private final Object m_o2;

		public Container(Object o1, Object o2)
		{
			m_o1 = o1;
			m_o2 = o2;
		}

		public Object getO1()
		{
			return m_o1;
		}

		public Object getO2()
		{
			return m_o2;
		}

		public boolean isBool()
		{
			return m_o1 != null;
		}

	}
}
