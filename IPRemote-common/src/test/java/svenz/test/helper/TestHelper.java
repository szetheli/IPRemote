/**
 * 
 */
package svenz.test.helper;

import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.easymock.Capture;

/**
 * @author Sven Zethelius
 *
 */
public class TestHelper
{
	private static Map<String, Level> LOGGER_LEVELS = new HashMap<>();

	public static void setLogger(String name, Level level)
	{
		Logger logger = Logger.getLogger(name);
		LOGGER_LEVELS.put(name, logger.getLevel());
		logger.setLevel(level);
	}

	public static void resetLoggers()
	{
		for (Map.Entry<String, Level> entry : LOGGER_LEVELS.entrySet())
		{
			Logger.getLogger(entry.getKey()).setLevel(entry.getValue());
		}
	}

	public static void waitCapture(Capture<?> capture) throws InterruptedException
	{
		waitCapture(capture, 1);
	}

	public static void waitCapture(Capture<?> capture, int count) throws InterruptedException
	{
		// TODO latched capture instead of polling?
		for (int i = 0; i < 100 && !capture.hasCaptured() && capture.getValues().size() < count; i++)
			Thread.sleep(10);
	}

}
