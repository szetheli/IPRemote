/**
 *
 * LoggingExceptionHandler.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.common.utilities;


import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.LoggerFactory;

/**
 * LoggingExceptionHandler installs the default {@link UncaughtExceptionHandler} that logs any exception to occur.
 * 
 * @author Sven Zethelius
 * 
 */
public class LoggingExceptionHandler
{
	public static void init()
	{
		Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
	}

	private static class LoggingUncaughtExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler
	{
		@Override
		public void uncaughtException(Thread t, Throwable e)
		{
			StackTraceElement[] stackTrace = e.getStackTrace();
			String className = null;
			for (StackTraceElement element : stackTrace)
			{
				if (element.getClassName().startsWith("svenz"))
					className = element.getClassName();
				break;
			}
			if (className == null)
				className = Utilities.class.getName();
			LoggerFactory.getLogger(className).error("Uncaught exception", e);
		}
	}

}
