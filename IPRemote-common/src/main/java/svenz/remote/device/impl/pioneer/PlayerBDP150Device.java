/**
 * 
 */
package svenz.remote.device.impl.pioneer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.xml.namespace.QName;
import org.apache.commons.configuration.Configuration;
import svenz.remote.common.utilities.LoggingRunnable;
import svenz.remote.device.IChangable.IChangeListener;
import svenz.remote.device.IMenu;
import svenz.remote.device.IPlayable;
import svenz.remote.device.IPlayable.Action;
import svenz.remote.device.ipremote.AbstractCoded;
import svenz.remote.device.ipremote.AbstractQueuingDevice;
import svenz.remote.device.ipremote.ChannelDeviceListener;
import svenz.remote.device.ipremote.MenuImpl;
import svenz.remote.device.ipremote.PlayableImpl;
import svenz.remote.device.ipremote.PoweredImpl;
import svenz.remote.device.ipremote.QueryChangeListener;
import svenz.remote.device.ipremote.QueuingWritableByteChannel;
import svenz.remote.device.ipremote.SelectableImpl;
import svenz.remote.net.protocol.ssdp.MatchingDeviceListener;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;

/**
 * @author Sven Zethelius
 *
 */
public class PlayerBDP150Device extends AbstractQueuingDevice
{
	private transient ScheduledExecutorService m_executor;
	private final PoweredImpl m_powered = new GracefulPoweredImpl();
	private final MenuImpl m_menuDisk = new MenuImpl();
	private final MenuImpl m_menuMain = new MenuImpl();
	private final PlayableImpl m_playable = new PlayableImpl();
	private final PlayerInput m_input = new PlayerInput();
	private final SelectableImpl m_disk = new SelectableImpl();
	private final Map<String, IPlayable.Action> m_playActions = new HashMap<String, IPlayable.Action>();
	private final Map<String, String> m_diskStates = new HashMap<String, String>();

	private transient final Runnable m_powerQueryRunnable = new LoggingRunnable(new QueryRunnable(m_powered));
	private transient final Runnable m_diskQueryRunnable = new LoggingRunnable(new QueryRunnable(m_disk));
	private transient Future<?> m_busyPolling = new FutureTask<Void>(m_powerQueryRunnable, null);
	private transient Future<?> m_diskPolling = m_busyPolling;
	private long m_busyQueryDelayMS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);

	public PlayerBDP150Device()
	{
		DiskChangeListener diskChangeListener = new DiskChangeListener();
		TrayOpenListener trayOpenListener = new TrayOpenListener();

		Configuration codes = getConfiguration();
		m_powered.setName("BD.powered");
		m_powered.setCodes(codes.subset("power"));
		m_powered.addChangeListener(new QueryChangeListener(m_playable, m_disk, m_input));

		m_menuDisk.setName("BD.menuDisk");
		initMenu(m_menuDisk, codes.subset("menu"), codes.subset("menu.disk"));

		m_menuMain.setName("BD.menuMain");
		initMenu(m_menuMain, codes.subset("menu"), codes.subset("menu.main"));

		m_playable.setName("BD.playable");
		m_playable.setCodes(codes.subset("play"));

		m_input.setName("BD.input");
		m_input.setCodes(codes.subset("input"));
		m_input.addChangeListener(trayOpenListener);

		m_disk.setName("BD.disk");
		m_disk.setCodes(codes.subset("disk"));
		m_disk.addChangeListener(diskChangeListener);
		m_disk.addChangeListener(trayOpenListener);

		m_busyPolling.cancel(false); // set future's state to cancelled

		initPlayResponses(codes.subset("play.Query.response"));
		initDiskStates(codes.subset("disk.Query.response"));

		register(m_powered, m_menuDisk, m_menuMain, m_playable, m_disk, m_input);
	}

	@Override
	public void setExecutor(ScheduledExecutorService executor)
	{
		super.setExecutor(executor);
		m_executor = executor;
	}

	public void setBusyQueryDelay(long busyQueryDelay, TimeUnit unit)
	{
		m_busyQueryDelayMS = TimeUnit.MILLISECONDS.convert(busyQueryDelay, unit);
	}

	@Override
	public void close() throws IOException
	{
		m_busyPolling.cancel(false);
		super.close();
	}

	private void initMenu(MenuImpl menu, Configuration codes, Configuration codes2)
	{
		menu.setCodes(codes);
		menu.setCodes(codes2);
		menu.filterCodes(new HashSet<String>(
				Arrays.asList("Menu", "Enter", "Back", "Exit", "Up", "Down", "Left", "Right")));
	}

	private void initPlayResponses(Configuration codes)
	{
		for (Iterator<String> iter = codes.getKeys(); iter.hasNext();)
		{
			String key = iter.next();
			m_playActions.put(codes.getString(key), Action.valueOf(key));
		}
	}

	private void initDiskStates(Configuration codes)
	{
		for (Iterator<String> iter = codes.getKeys(); iter.hasNext();)
		{
			String key = iter.next();
			m_diskStates.put(codes.getString(key), key);
		}
	}

	public MenuImpl getMenuDisk()
	{
		return m_menuDisk;
	}

	public MenuImpl getMenuMain()
	{
		return m_menuMain;
	}

	public PlayableImpl getPlayable()
	{
		return m_playable;
	}

	public PoweredImpl getPowered()
	{
		return m_powered;
	}

	public SelectableImpl getInput()
	{
		return m_input;
	}

	@Override
	protected IDeviceListener<?> initDeviceListener(ChannelDeviceListener listener)
	{
		listener.setElementName(new QName("http://www.pioneerelectronics.com/xmlns/av", "X_ipRemoteTcpPort"));

		MatchingDeviceListener<?> match = new MatchingDeviceListener<Object>(listener);
		match.setDeviceType("urn:pioneer-co-jp:device:PioControlServer:1");
		match.setFriendlyName("BDP-150");
		match.setModelName("BDP-150/UCXESM");
		return match;
	}
	
	
	@Override
	protected boolean handleResponse(String response, String lastCode)
	{
		IPlayable.Action action;
		String diskState, inputState;
		if ("R".equals(response))
		{
			return processSuccess(lastCode);
		}
		else if ("BDP-150".equals(response))
		{
			clearPending(lastCode);
			return processSuccess(lastCode);
		}
		else if (response.startsWith("E"))
		{
			processFailure(lastCode, response);
			return false;
		}
		else if ("Query".equals(m_playable.getCommand(lastCode)))
		{
			if (null != (action = m_playActions.get(response)))
			{
				clearPending(lastCode);
				return processPlayState(action);
			}
			else
			{ // we can get corrupt responses from the play query if it happens too soon after power on
				getLogger().warn("Unknown response to play query.  Response:{}", response);
				retryQuery(m_playable);
				return false;
			}
		}
		else if ("Query".equals(m_disk.getCommand(lastCode)))
		{ // we have a disk state
			if (null != (diskState = m_diskStates.get(response)))
			{
				clearPending(lastCode);
				m_disk.setStatus(diskState);
				return true;
			}
			else
			{
				getLogger().warn("Unknown response to disk query.  Response:{}", response);
				retryQuery(m_disk);
				return false;
			}
		}
		else if (null != (inputState = m_input.getCommand(lastCode)))
		{
			if ("Query".equals(inputState))
			{
				clearPending(lastCode);
				m_input.setStatus("dvd"); // Query success is DVD/BD mode
			}
			else
				m_input.setStatus(inputState); // note that gives NO useful state info when in netflix mode
		}
		else if ("Query".equals(m_playable.getCommand(lastCode)))
		{

		}
		// TODO more states
		else
			getLogger().warn("Unknown response to command {}.\nResponse:{}", lastCode, response);
		return false;
	}

	private void retryQuery(AbstractCoded<?> coded)
	{
		QueuingWritableByteChannel channel = getQueuingWritableByteChannel();
		channel.setRetryDelayMS(TimeUnit.SECONDS.toMillis(5));
		coded.query();
		channel.setRetryDelayMS(-1);
	}

	private boolean processPlayState(Action action)
	{
		// play state means we are in a powered mode.
		getPowered().setStatus(true);
		endBusy(m_busyPolling);

		if (Action.Play.equals(action))
			m_playable.setStatus(IPlayable.Action.Play);
			// TODO transition to Play should include enter to remove BDLive message (if BD)
		else if (Action.Stop.equals(action))
			m_playable.setStatus(IPlayable.Action.Stop);
		else if (Action.Pause.equals(action))
			m_playable.setStatus(IPlayable.Action.Pause);
		else if (Action.FastForward.equals(action) || Action.Rewind.equals(action)) // FastForward or Rewind x2, x4, x8
			; // TODO check position to determine state?
		else if (Action.NextTrack.equals(action) || Action.PreviousTrack.equals(action))
			; // TODO do nothing? reset any FF/Rev state
		else
		{
			getLogger().warn("Unhandled play status:{}", action);
			m_playable.setStatus(null);
			m_playable.query();
		}
		// TODO During Netflix, everything is state stopped.
		return true;
	}

	private void processFailure(String lastCode, String response)
	{
		String power, play, disk, input;
		if ("E04".equals(response))
		{
			if("On".equals(power = m_powered.getCommand(lastCode)))
			{ // failed on powerOn - we are already on.
				m_powered.setStatus(true);
				return;
			}
			else if ("Off".equals(power))
			{
				m_powered.setStatus(false);
				return;
			}
			else if (!m_busyPolling.isCancelled())
			{ // we are already polling for power state
				return; // do nothing,
			}
			else if ("Query".equals(input = m_input.getCommand(lastCode)))
			{
				clearPending(lastCode);
				// input Query failed, means we don't know input, implies one of web contents, e.g. netflix
				// TODO other input states
				if (m_input.getStatus() == null || "dvd".equals(m_input.getStatus()))
					m_input.setStatus("netflix");
				return;
			}
			else if ("Query".equals(power)
					|| "Query".equals(play = m_playable.getCommand(lastCode))
					|| "Query".equals(disk = m_disk.getCommand(lastCode)))
			{ // query failed and we aren't busy
				m_powered.setStatus(false);
				clearPending();
				return;
			}
		}
		// TODO other failure modes
		getLogger().error("Unhandled failure: {} {}", response, lastCode);
	}

	private boolean processSuccess(String lastCode)
	{
		String command = m_powered.getCommand(lastCode), input;
		if ("Query".equals(command))
		{
			endBusy(m_busyPolling);
			m_powered.setStatus(true);
			return true;
		}
		else if ("On".equals(command))
		{ // successful power on means we now need to wait for the player to initialize.
			// there is a period when it won't respond to commands while loading.
			startBusy();
			return true;
		}
		else if ("Off".equals(command))
		{
			endBusy(m_busyPolling);
			m_powered.setStatus(false);
			return true;
		}
		else if (null != (command = m_playable.getCommand(lastCode)))
			return processPlayState(Action.valueOf(command));
		else if (null != (command = m_disk.getCommand(lastCode)))
			m_disk.setStatus(command);
		else if (null != m_menuDisk.getCommand(lastCode))
			; // TODO menu tracking?
		else if (null != m_menuMain.getCommand(lastCode))
			; // TODO menu tracking?
		else if (null != (input = m_input.getCommand(lastCode)))
			m_input.setStatus(input);
		else
		{
			getLogger().warn("Unhandled success: {}", lastCode);
			return false;
		}
		return true;
	}

	private void startBusy()
	{
		getLogger().trace("Starting polling for busy state");
		m_busyPolling =
				m_executor.scheduleWithFixedDelay(
						m_powerQueryRunnable, 
						m_busyQueryDelayMS, 
						m_busyQueryDelayMS,
						TimeUnit.MILLISECONDS);
	}
	
	private void startLoading()
	{
		getLogger().trace("Starting polling for disk state");
		m_diskPolling =
				m_executor.scheduleWithFixedDelay(
						m_diskQueryRunnable, 
						m_busyQueryDelayMS, 
						m_busyQueryDelayMS,
						TimeUnit.MILLISECONDS);
	}

	private void endBusy(Future<?> future)
	{
		if (future.cancel(false))
			getLogger().trace("Ending polling");
	}

	/**
	 * Runnable for periodically polling an AbstractCoded
	 * 
	 * @author Sven Zethelius
	 * 
	 */
	private static class QueryRunnable implements Runnable
	{
		private final AbstractCoded<?>[] m_coded;

		public QueryRunnable(AbstractCoded<?>... coded)
		{
			m_coded = coded;
		}

		@Override
		public void run()
		{
			for (AbstractCoded<?> coded : m_coded)
				coded.query();
		}
	}

	/**
	 * Poll the disk state when we enter one of the intermediate states (Loading, Open, Close)
	 * 
	 * @author Sven Zethelius
	 * 
	 */
	private class DiskChangeListener implements IChangeListener
	{
		@Override
		public void stateChanged(Object target, String property)
		{
			String diskStatus = m_disk.getStatus();
			if ("Loading".equals(diskStatus) || "Open".equals(diskStatus))
			{
				startLoading();
				return;
			}
			else if ("Close".equals(diskStatus))
			{
				m_disk.query();
			}
			else
			{
				endBusy(m_diskPolling);
			}
		}
	}

	private class PlayerInput extends SelectableImpl
	{
		@Override
		public void setSelection(String input)
		{
			if (!"netflix".equals(input) && "netflix".equals(getStatus()))
			{ // netflix needs to be "gracefully" exited or all hell breaks loose.
				QueuingWritableByteChannel channel = getQueuingWritableByteChannel();
				channel.setRetryDelayMS(TimeUnit.SECONDS.toMillis(30));
				m_playable.setAction(IPlayable.Action.Stop);
				m_menuMain.action(IMenu.Action.Exit);
				channel.setRetryDelayMS(-1);
			}
			if ("dvd".equals(input))
				setStatus("dvd"); // no codes to send
			// TODO any way to exit netflix safely?
			else
				super.setSelection(input);
		}
	}

	/**
	 * Clear selection before powering off in order to exit netflix
	 * 
	 * @author Sven Zethelius
	 * 
	 */
	private class GracefulPoweredImpl extends PoweredImpl
	{
		@Override
		public void powerOff()
		{
			m_input.setStatus(null);
			super.powerOff();
		}
	}

	/**
	 * Cause the tray to open when we reach an empty disk state while trying to use dvd input
	 * 
	 * @author Sven Zethelius
	 * 
	 */
	private class TrayOpenListener implements IChangeListener
	{
		@Override
		public void stateChanged(Object target, String property)
		{
			String disk = m_disk.getStatus();
			String input = m_input.getStatus();
			if (disk == null || input == null)
				return; // not enough information
			if ("Empty".equals(disk) && ("dvd".equals(input))) // empty
				m_disk.setSelection("Open");
		}
	}
}

