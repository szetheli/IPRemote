/**
 * 
 */
package svenz.remote.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import svenz.remote.device.IChangable.IChangeListener;
import svenz.remote.device.IPowered;
import svenz.remote.device.IPowered.IPoweredAware;

/**
 * @author Sven Zethelius
 *
 */
public class PowerAnimator implements IPoweredAware, IChangeListener
{
	// 3800 lower edge of ball,
	// 6900 upper edge of ball,
	// 9950 upper edge of stick
	private IClipImage m_ringOff;
	private IClipImage m_ringOn;
	private IClipImage m_stickOff;
	private IClipImage m_stickOn;
	// assumes a background of ringOff and stickOff with ringOn/stickOn, ringOff/stickOff

	private final Collection<IPowered> m_powereds = new ArrayList<IPowered>(4);
	private Boolean m_desiredState;
	private Executor m_executor;
	private final Runnable m_uiRunnableStateChange = new UIRunnable(true);
	private final Runnable m_uiRunnableToggle = new UIRunnable(false);

	public void setExecutor(Executor executor)
	{
		m_executor = executor;
	}

	public void setRingOff(IClipImage ringOff)
	{
		m_ringOff = ringOff;
	}

	public void setRingOn(IClipImage ringOn)
	{
		m_ringOn = ringOn;
	}

	public void setStickOff(IClipImage stickOff)
	{
		m_stickOff = stickOff;
	}

	public void setStickOn(IClipImage stickOn)
	{
		m_stickOn = stickOn;
	}

	@Override
	public void addPowered(Collection<IPowered> powereds)
	{
		synchronized (this)
		{
			m_desiredState = null; // we don't know
			m_powereds.addAll(powereds);
			for (IPowered powered : powereds)
				powered.addChangeListener(this);
		}
		m_executor.execute(m_uiRunnableStateChange);
	}

	@Override
	public void removePowered(java.util.Collection<IPowered> powereds)
	{
		synchronized (this)
		{
			m_powereds.removeAll(powereds);
			for (IPowered powered : powereds)
				powered.removeChangeListener(this);
		}
		// TODO schedule an update to power off devices not in use
	};

	public void toggle()
	{
		synchronized (this)
		{
			m_desiredState = m_desiredState == Boolean.TRUE ? Boolean.FALSE : Boolean.TRUE;
			m_executor.execute(m_uiRunnableToggle);
			for (IPowered powered : m_powereds)
			{
				if (m_desiredState == Boolean.TRUE)
					powered.powerOn();
				else
					powered.powerOff();
			}

		}
		// TODO schedule state checkup
	}

	@Override
	public void stateChanged(Object target, String property)
	{
		m_executor.execute(m_uiRunnableStateChange);
	}

	private void setRingLevel(IClipImage left, int leftAmount, IClipImage right, int rightAmount)
	{
		int leftLevel, rightLevel;
		if (rightAmount == 0 && leftAmount == 0)
		{
			leftLevel = 0;
			rightLevel = 0;
		}
		else
		{
			leftLevel = 10000 * leftAmount / (leftAmount + rightAmount);
			rightLevel = right == m_ringOn ? 10000 : 0;
		}

		left.setLevel(leftLevel);
		right.setLevel(rightLevel);
	}

	private void update(boolean changeDesiredState)
	{
		synchronized (this)
		{

			int off = 0, on = 0;
			for (IPowered powered : m_powereds)
			{
				if (powered.isPowered())
					on++;
				else
					off++;
			}

			if (m_desiredState == null || changeDesiredState)
			{
				if (on == m_powereds.size())
					m_desiredState = Boolean.TRUE;
				else if (off == m_powereds.size())
					m_desiredState = Boolean.FALSE;
			}

			if (m_desiredState == null)
			{ // mixed display: stick ball OFF, stick length = ON, ring left=ON,right=OFF
				m_stickOff.setLevel(6900);
				m_stickOn.setLevel(10000);
				setRingLevel(m_ringOn, on, m_ringOff, off);
			}
			else if (m_desiredState == Boolean.TRUE)
			{ // stick ball ON, stick length ON, ring left=ON,right=OFF
				m_stickOff.setLevel(0);
				m_stickOn.setLevel(10000);
				setRingLevel(m_ringOn, on, m_ringOff, off);
			}
			else
			// if (m_desiredState == Boolean.FALSE)
			{ // stick ball OFF, stick length OFF, ring left=OFF, right=ON
				m_stickOff.setLevel(0);
				m_stickOn.setLevel(0);
				setRingLevel(m_ringOff, off, m_ringOn, on);
			}
		}
	}

	private class UIRunnable implements Runnable
	{
		private final boolean m_changeDesiredState;

		public UIRunnable(boolean changeDesiredState)
		{
			super();
			m_changeDesiredState = changeDesiredState;
		}

		@Override
		public void run()
		{
			update(m_changeDesiredState);
		}
	}

}
