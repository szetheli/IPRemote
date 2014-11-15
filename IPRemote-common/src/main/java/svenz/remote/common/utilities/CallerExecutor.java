/**
 * 
 */
package svenz.remote.common.utilities;

import java.util.concurrent.Executor;

/**
 * Executor implementation that runs in caller thread
 * 
 * @author Sven Zethelius
 * 
 */
public class CallerExecutor implements Executor
{
	public static final CallerExecutor INSTANCE = new CallerExecutor();

	@Override
	public void execute(Runnable command)
	{
		command.run();
	}
}
