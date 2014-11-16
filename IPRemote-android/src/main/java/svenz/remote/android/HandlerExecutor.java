/**
 * 
 */
package svenz.remote.android;

import java.util.concurrent.Executor;
import android.os.Handler;

/**
 * @author Sven Zethelius
 *
 */
public class HandlerExecutor implements Executor
{
	private final Handler m_handler;

	public HandlerExecutor(Handler handler)
	{
		m_handler = handler;
	}

	@Override
	public void execute(Runnable command)
	{
		m_handler.post(command);
	}
}
