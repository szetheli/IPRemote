/**
 * 
 */
package svenz.remote.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.device.IPowered;
import svenz.remote.device.IPowered.IPoweredAware;

/**
 * When run, sets {@link IPowered#isPowered()} to a consistent state across all {@link IPowered} registered with
 * {@link #addPowered(Collection)}. If all devices are consistent, toggle state.
 * 
 * @author Sven Zethelius
 * 
 */
public class PowerToggleAction implements Runnable, IPoweredAware
{

	private static final Logger LOGGER = LoggerFactory.getLogger(PowerToggleAction.class);
	private final Collection<IPowered> m_powereds = new ConcurrentLinkedQueue<IPowered>();

	@Override
	public void addPowered(Collection<IPowered> powered)
	{
		LOGGER.trace("Adding powered {}", powered);
		m_powereds.addAll(powered);
	}

	@Override
	public void removePowered(Collection<IPowered> powered)
	{
		LOGGER.trace("Removing powered {}", powered);
		m_powereds.removeAll(powered);
	}

	@Override
	public void run()
	{
		int on = 0, off = 0;
		Collection<IPowered> powereds = new ArrayList<IPowered>(m_powereds);
		int size = powereds.size();
		for (IPowered powered : powereds)
		{
			if (powered.isPowered())
				on++;
			else
				off++;
		}
		
		// TODO - change to match Android impl
		// sends the powerOn/powerOff even if already in that state, just in case actual state is incorrect
		if ((on > 0 && on < size) || (off == powereds.size()))
		{ // some but not all on, or all off
			for (IPowered powered : powereds)
				powered.powerOn();
		}
		else
		{// some but not all off, or all on
			for (IPowered powered : powereds)
				powered.powerOff();
		}
	}

}
