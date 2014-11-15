/**
 *
 * LoggingProxy.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.common.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoggingProxy proxies an interface, logging each method called on it.
 * 
 * @author Sven Zethelius
 * 
 */
public class LoggingProxy
{
	/**
	 * Get a proxy of he interface.
	 * 
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getProxy(Class<T> clazz)
	{
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, new LoggingHandler(
				clazz, null));
	}

	/**
	 * Get a proxy that wraps an existing instance
	 * 
	 * @param t
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getProxy(T t)
	{
		Class<T> clazz = (Class<T>) t.getClass();
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, new LoggingHandler(
				clazz, t));
	}

	/**
	 * enable stack tracing of the interface calls
	 * 
	 * @param t
	 * @return
	 */
	public static <T> T trace(T t)
	{
		LoggingHandler handler = (LoggingHandler) Proxy.getInvocationHandler(t);
		handler.m_trace = true;
		return t;
	}

	private static class LoggingHandler implements InvocationHandler
	{
		private final Logger m_logger;
		private final Object m_wrapped;
		private boolean m_trace;

		public LoggingHandler(Class<?> clazz, Object wrapped)
		{
			super();
			m_logger = LoggerFactory.getLogger(clazz);
			m_wrapped = wrapped;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] params) throws Throwable
		{
			Object ret = m_wrapped != null ? method.invoke(m_wrapped, params) : "";
			StringWriter sw = new StringWriter();
			sw.append("{}({})\t{}");
			if (m_trace)
			{
				sw.append("\n");
				new Exception().printStackTrace(new PrintWriter(sw));
			}

			m_logger.debug(sw.toString(), method.getName(), toString(params), toString(new Object[] { ret }));
			return ret;
		}

		private String toString(Object[] params)
		{
			StringBuilder sb = new StringBuilder();
			for (Object param : params)
			{
				sb.append(',');
				toString(param, sb);
			}
			return sb.substring(1);
		}

		private void toString(Object param, StringBuilder sb)
		{
			if (param instanceof Array)
			{
				sb.append('[');
				for (Object o : (Object[]) param)
				{
					toString(o, sb);
					sb.append(',');
				}
				sb.replace(sb.length() - 1, sb.length(), "]");
			}
			else
				sb.append(param);
		}

	}
}
