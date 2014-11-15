/**
 *
 * ScheduledThreadPoolExecutorConfigurer.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.common.thread;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Create to commonly config a {@link ScheduledThreadPoolExecutor}
 * 
 * @author Sven Zethelius
 * 
 */
public class ScheduledThreadPoolExecutorConfigurer
{
	private final ScheduledThreadPoolExecutor m_executor;

	public ScheduledThreadPoolExecutorConfigurer(ScheduledThreadPoolExecutor executor, long period, TimeUnit unit)
	{
		super();
		m_executor = executor;
		period = TimeUnit.MILLISECONDS.convert(period, unit);
		m_executor.setKeepAliveTime(period + 30, TimeUnit.MILLISECONDS);
		m_executor.allowCoreThreadTimeOut(true);
		m_executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		m_executor.scheduleAtFixedRate(new AdjustRunnable(), 0, period, unit);
	}

	private final class AdjustRunnable implements Runnable
	{
		@Override
		public void run()
		{
			int backlog = m_executor.getQueue().size();
			int coreSize = m_executor.getCorePoolSize();
			int maxSize = m_executor.getMaximumPoolSize();
			if (backlog > 0 && coreSize < maxSize)
			{ // increase core
				m_executor.setCorePoolSize(Math.min(maxSize, coreSize + backlog));
			}
			else if (backlog == 0 && coreSize > 1)
			{ // decrease core
				m_executor.setCorePoolSize(1);
			}
		}
	}

}
