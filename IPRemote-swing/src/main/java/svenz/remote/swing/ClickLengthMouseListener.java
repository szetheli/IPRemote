/**
 * 
 */
package svenz.remote.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import svenz.remote.common.utilities.LoggingRunnable;

/**
 * Fires a short click or long click action depending on how long the mouse press is held for.
 * 
 * @author Sven Zethelius
 * 
 */
public class ClickLengthMouseListener extends MouseAdapter
{
	private final Runnable m_trigger = new LoggingRunnable(new LongTrigger());
	private ScheduledExecutorService m_executor;
	private long m_longDelayMS;
	private ActionListener m_shortClick;
	private ActionListener m_longClick;
	private Future<?> m_future;
	private ActionEvent m_event;

	public void setExecutor(ScheduledExecutorService executor)
	{
		m_executor = executor;
	}

	public void setLongClick(ActionListener longClick)
	{
		m_longClick = longClick;
	}

	public void setShortClick(ActionListener shortClick)
	{
		m_shortClick = shortClick;
	}

	public void setLongDelay(long longDelay, TimeUnit unit)
	{
		m_longDelayMS = TimeUnit.MILLISECONDS.convert(longDelay, unit);
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		m_event = new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, null);
		m_future = m_executor.schedule(m_trigger, m_longDelayMS, TimeUnit.MILLISECONDS);
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (m_future != null && m_future.cancel(false))
			action(m_shortClick);
	}

	private void action(ActionListener listener)
	{
		listener.actionPerformed(m_event);
	}

	private class LongTrigger implements Runnable
	{
		@Override
		public void run()
		{
			m_future.cancel(false);
			action(m_longClick);
		}
	}
}
