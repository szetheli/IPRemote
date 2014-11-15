/**
 *
 * DaemonThreadFactory.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.common.thread;

import java.util.concurrent.ThreadFactory;

/**
 * DaemonThreadFactory wraps a thread factory, causing any threads created by it to be daemon
 * 
 * @author Sven Zethelius
 * 
 */
public class DaemonThreadFactory implements ThreadFactory
{
	private final ThreadFactory m_factory;

	public DaemonThreadFactory(ThreadFactory factory)
	{
		m_factory = factory;
	}

	@Override
	public Thread newThread(Runnable r)
	{
		Thread t = m_factory.newThread(r);
		t.setDaemon(true);
		return t;
	}

}
