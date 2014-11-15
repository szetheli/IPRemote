/**
 * 
 */
package svenz.test.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

/**
 * @author Sven Zethelius
 *
 */
public class CaptureAppender extends AppenderSkeleton
{
	private static final Map<Logger, CaptureAppender> LOGGERS = new HashMap<Logger, CaptureAppender>();
	private final List<LoggingEvent> m_events = new ArrayList<LoggingEvent>();

	public static CaptureAppender install(String... names)
	{
		CaptureAppender appender = new CaptureAppender();
		for (String name : names)
		{
			Logger logger = Logger.getLogger(name);
			logger.addAppender(appender);
			synchronized (LOGGERS)
			{
				LOGGERS.put(logger, appender);
			}
		}
		return appender;
	}

	public static void reset()
	{
		synchronized (LOGGERS)
		{
			for (Map.Entry<Logger, CaptureAppender> entry : LOGGERS.entrySet())
			{
				entry.getKey().removeAppender(entry.getValue());
			}
			LOGGERS.clear();
		}
	}

	public List<LoggingEvent> getEvents()
	{
		return m_events;
	}

	@Override
	public void close()
	{

	}

	@Override
	public boolean requiresLayout()
	{
		return false;
	}

	@Override
	protected void append(LoggingEvent event)
	{
		synchronized (this)
		{
			if(!m_events.contains(event)) // prevent logger adds from being in the heirarchy multiple times.
				m_events.add(event);
		}
	}
}
