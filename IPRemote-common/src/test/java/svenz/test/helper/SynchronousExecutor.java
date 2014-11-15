/**
 * 
 */
package svenz.test.helper;

import java.util.concurrent.Executor;

/**
 * @author Sven Zethelius
 *
 */
public class SynchronousExecutor implements Executor
{

	@Override
	public void execute(Runnable command)
	{
		command.run();
	}

}
