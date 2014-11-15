/**
 *
 * SimpleNameThreadFactory.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.common.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SimpleNameThreadFactory wraps a {@link ThreadFactory} and sets it name
 * 
 * @author Sven Zethelius
 * 
 */
public class NameThreadFactory implements ThreadFactory
{
	private final ThreadFactory m_factory;
	private final String m_name;
	private final AtomicInteger m_index = new AtomicInteger(0);

	public NameThreadFactory(ThreadFactory factory, String name)
	{
		m_factory = factory;
		m_name = name;
	}

	@Override
	public Thread newThread(Runnable r)
	{
		Thread t = m_factory.newThread(r);
		t.setName(m_name + m_index.incrementAndGet());
		return t;
	}

}
