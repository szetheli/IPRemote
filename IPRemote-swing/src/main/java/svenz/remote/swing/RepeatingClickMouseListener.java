/**
 * 
 */
package svenz.remote.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import svenz.remote.common.utilities.LoggingRunnable;

/**
 * Fires an action event on a repeating timer so long as the mouse is held down
 * 
 * @author Sven Zethelius
 * 
 */
public class RepeatingClickMouseListener extends MouseAdapter
{
	private final Runnable m_runnable = new LoggingRunnable(new ClickRunnable());
	private ActionListener m_listener;
	private ScheduledExecutorService m_executor;
	private long m_delayMS;
	private RunnableFuture<?> m_future;
	private MouseEvent m_event;

	public void setDelay(long delay, TimeUnit unit)
	{
		m_delayMS = TimeUnit.MILLISECONDS.convert(delay, unit);
	}

	public void setExecutor(ScheduledExecutorService executor)
	{
		m_executor = executor;
	}

	public void setListener(ActionListener listener)
	{
		m_listener = listener;
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		m_event = e;
		m_future =
				(RunnableFuture<?>) m_executor.scheduleWithFixedDelay(
						m_runnable, 
						m_delayMS, 
						m_delayMS,
						TimeUnit.MILLISECONDS);
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (!m_future.isDone())
		{
			m_future.cancel(false);
			m_runnable.run();
		}
		else
		{
			m_future.cancel(false);
		}

		m_future = null;
		m_event = null;
	}
	
	private class ClickRunnable implements Runnable
	{
		@Override
		public void run()
		{
			m_listener.actionPerformed(new ActionEvent(
					m_event.getSource(), 
					ActionEvent.ACTION_PERFORMED, 
					null, 
					m_event.getWhen(), 
					m_event.getModifiers()));
		}
	}

}
