/**
 * 
 */
package svenz.remote.device.impl.sharp;

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.apache.commons.configuration.Configuration;
import svenz.remote.device.IMenu;
import svenz.remote.device.IPowered;
import svenz.remote.device.ISelectable;
import svenz.remote.device.ISound;
import svenz.remote.device.ipremote.AbstractQueuingDevice;
import svenz.remote.device.ipremote.ChannelDeviceListener;
import svenz.remote.device.ipremote.MenuImpl;
import svenz.remote.device.ipremote.PlayableImpl;
import svenz.remote.device.ipremote.PoweredImpl;
import svenz.remote.device.ipremote.SelectableImpl;
import svenz.remote.device.ipremote.SoundImpl;
import svenz.remote.net.protocol.ssdp.CompositeDeviceListener;
import svenz.remote.net.protocol.ssdp.MatchingDeviceListener;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;
import svenz.remote.net.protocol.ssdp.WrapperDeviceListener;
import svenz.remote.net.protocol.ssdp.jaxb.Device;

/**
 * @author Sven Zethelius
 *
 */
public class TVAquas60LE650Device extends AbstractQueuingDevice
{

	private final PoweredImpl m_powered = new PoweredImpl();
	private final MenuImpl m_menu = new MenuImpl();
	private final SoundImpl m_sound = new SoundImpl();
	private final SelectableImpl m_input = new SelectableImpl();
	private final PlayableImpl m_play = new PlayableImpl();
	private final int m_ipRemotePort;

	public TVAquas60LE650Device()
	{
		Configuration codes = getConfiguration();

		m_powered.setName("TV.powered");
		m_powered.setCodes(codes.subset("power"));

		m_menu.setName("TV.menu");
		m_menu.setCodes(codes.subset("menu"));

		m_sound.setName("TV.sound");
		m_sound.setCodes(codes.subset("sound"));

		m_play.setName("TV.play");
		m_play.setCodes(codes.subset("play"));

		m_input.setName("TV.input");
		initSelectable(m_input, codes.subset("input"), codes.subset("input"));

		m_ipRemotePort = codes.getInt("Port");

		register(m_powered, m_menu, m_sound, m_input, m_play);
	}

	int getIPRemotePort()
	{
		return m_ipRemotePort;
	}

	public ISelectable getInput()
	{
		return m_input;
	}

	public IMenu getMenu()
	{
		return m_menu;
	}

	public IPowered getPowered()
	{
		return m_powered;
	}

	public ISound getSound()
	{
		return m_sound;
	}

	public PlayableImpl getPlayable()
	{
		return m_play;
	}

	@Override
	protected IDeviceListener<?> initDeviceListener(ChannelDeviceListener listener)
	{
		listener.setPort(m_ipRemotePort);

		MatchingDeviceListener<Object> match = new MatchingDeviceListener<Object>(listener);
		match.setDeviceType("urn:schemas-sharp-co-jp:device:AquosIPC:1");
		match.setFriendlyName("AQUOS LE650U");
		match.setModelName("AQUOS \nLC-60LE650U");

		IDeviceListener<Object> netflix = new NetflixDeviceListener<Object>(listener);
		return new CompositeDeviceListener<Object>(Arrays.asList(match, netflix));
	}


	@Override
	protected boolean handleResponse(String response, String lastCode)
	{
		if ("OK".equals(response) || "AQUOS LE650U".equals(response))
		{
			processSuccess(lastCode);
			return true;
		}
		else if ("ERR".equals(response))
		{
			processError(lastCode);
			return false;
		}
		else
		{
			getLogger().error("Unknown error response '{}' to code '{}'", response, lastCode);
			return false;
		}
	}

	private void processSuccess(String lastCode)
	{
		String command, input, menu, play;
		if ("On".equals(command = m_powered.getCommand(lastCode)))
			m_powered.setStatus(true); // TODO set volume to "good" value
		else if ("Query".equals(command))
			m_powered.setStatus(true);
		else if ("Off".equals(command))
			m_powered.setStatus(false);
		else if (null != (input = m_input.getCommand(lastCode)))
			m_input.setStatus(input);
		else if (null != (menu = m_menu.getCommand(lastCode)))
			;// menus don't track status.
		else if (null != (play = m_play.getCommand(lastCode)))
		{
			// TODO handle play state
		}
		else
			getLogger().warn("Unknown success response '{}'", lastCode);
	}

	private void processError(String lastCode)
	{
		String command, input;
		if ("Query".equals(command = m_powered.getCommand(lastCode)))
			m_powered.setStatus(false);
		else if ("On".equals(command) || "Off".equals(command))
		{
			m_powered.setStatus(null);
			m_powered.query();
		}
		else if (m_powered.isPowered() && m_input.getStatus() == null && null != (input = m_input.getCommand(lastCode)))
		{
			// error when powered and no input status means we most likely are already on that input.
			m_input.setStatus(input);
		}
		// TODO more errors
		else
			getLogger().error("Unable to handle response for '{}'", lastCode);
	}

	/**
	 * Adapter because TV lies about SSDP information when netflix is enabled. Instead it gives a JSON response on the
	 * LOCATION in the SSDP message.
	 * 
	 * @author Sven Zethelius
	 * 
	 * @param <T>
	 */
	private static class NetflixDeviceListener<T> extends WrapperDeviceListener<T>
	{
		private static final int NETFLIX_PORT = 8090;

		public NetflixDeviceListener(IDeviceListener<T> listener)
		{
			super(listener);
		}

		@Override
		public T deviceAdded(Device device, InetSocketAddress remoteAddress)
		{
			if (device.getUDN() == null && remoteAddress.getPort() == NETFLIX_PORT)
				return super.deviceAdded(device, remoteAddress);
			else
				return null;
		}

	}
}
