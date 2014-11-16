/**
 * 
 */
package svenz.remote.android;

import java.util.concurrent.Executor;
import svenz.remote.common.utilities.CallerExecutor;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * @author Sven Zethelius
 *
 */
public class RunnableOnClickListener implements OnClickListener
{
	private final Executor m_executor;
	private final Runnable m_runnable;

	public RunnableOnClickListener(Runnable runnable)
	{
		this(runnable, CallerExecutor.INSTANCE);
	}

	public RunnableOnClickListener(Runnable runnable, Executor executor)
	{
		super();
		m_runnable = runnable;
		m_executor = executor;
	}

	@Override
	public void onClick(View v)
	{
		m_executor.execute(m_runnable);
	}


}
