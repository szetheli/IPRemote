/**
 * Utilities.java
 * Copyright 2013, Sven Zethelius
 * 
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package svenz.remote.common.utilities;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.MapTransformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.event.EventSource;
import org.slf4j.LoggerFactory;
/**
 * @author Sven Zethelius
 *
 */
public class Utilities
{

	public static void safeClose(Closeable toClose)
	{
		try
		{
			if (toClose != null)
				toClose.close();
		}
		catch (IOException e)
		{
			LoggerFactory.getLogger(toClose.getClass()).error("Exception closing {}", toClose, e);
		}
	}

	public static void safeClose(Socket toClose) // grr socket is not Closeable
	{
		try
		{
			if (toClose != null)
				toClose.close();
		}
		catch (IOException e)
		{
			LoggerFactory.getLogger(toClose.getClass()).error("Exception closing {}", toClose, e);
		}
	}

	public static void safeClose(Selector selector) // grr socket is not Closeable until JDK1.7
	{
		try
		{
			if (selector != null)
				selector.close();
		}
		catch (IOException e)
		{
			LoggerFactory.getLogger(selector.getClass()).error("Exception closing {}", selector, e);
		}
	}

	public static Class<?> getClassForLogging(Object o)
	{
		Class<?> clazz = o.getClass();
		while (clazz.isAnonymousClass())
			clazz = clazz.getEnclosingClass();
		return clazz;
	}

	public static void configure(ScheduledThreadPoolExecutor executor, long period, TimeUnit unit)
	{
		executor.setKeepAliveTime(period, unit);
		executor.allowCoreThreadTimeOut(true);
		executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
	}

	public static void sleep(long t)
	{
		try
		{
			Thread.sleep(t);
		}
		catch (InterruptedException e)
		{
			throw new IllegalStateException("Sleep interrupted", e);
		}
	}

	/**
	 * Wrap a Transformer in order to cache the result of a transform so we only have to do it once
	 * 
	 * @param t
	 *            transformer to wrap
	 * @return wrapped transform
	 */
	public static Transformer cacheTransformer(Transformer t)
	{
		return MapTransformer.getInstance(LazyMap.decorate(new HashMap<Object, Object>(), t));
	}

	/**
	 * Merge two ordered lists, based on the orderListDefault
	 * 
	 * @param existingList
	 * @param orderListDefault
	 * @return the updated list
	 */
	public static List<String> mergeOrderedLists(
			Collection<String> existingList, 
			List<String> orderListDefault)
	{
		existingList = new LinkedHashSet<String>(existingList);
		List<String> list = new ArrayList<String>(orderListDefault.size() + existingList.size());

		// intersection(orderList, existingList)
		// elements in orderList ^ elements in existingList
		// elements in existingList ^ elements in orderList
		for (String s : orderListDefault)
		{
			if (existingList.remove(s))
				list.add(s);
		}
		list.addAll(existingList);

		return list;
	}

	/**
	 * Set the configuration to autosave in another thread
	 * 
	 * @param config
	 * @param executor
	 */
	public static void setAsyncSaveConfiguration(final FileConfiguration config,
			final ScheduledExecutorService executor, final long delay, final TimeUnit unit)
	{
		EventSource s = (EventSource) config;
		final Runnable r = new SaveConfigRunnable(config);
		s.addConfigurationListener(new ConfigurationListener() {
			Future<?> m_future;

			@Override
			public void configurationChanged(ConfigurationEvent event)
			{
				if (event.isBeforeUpdate())
					return;

				synchronized (this)
				{
					// don't bother to save on consecutive updates until the last
					if (m_future != null && !m_future.isDone())
						m_future.cancel(false);
					m_future = executor.schedule(r, delay, unit);
				}
			}
		});

	}

	private static class SaveConfigRunnable implements Runnable
	{
		private final FileConfiguration m_config;

		public SaveConfigRunnable(FileConfiguration config)
		{
			m_config = config;
		}

		@Override
		public void run()
		{
			try
			{
				synchronized (SaveConfigRunnable.this)
				{
					m_config.save();
				}
			}
			catch (ConfigurationException e)
			{
				LoggerFactory.getLogger(getClassForLogging(m_config)).error("Exception saving file {}",
						m_config.getURL(), e);
			}
		}
	}
}
