/**
 * 
 */
package svenz.remote.device.impl.pioneer;

import javax.xml.namespace.QName;
import org.apache.commons.configuration.Configuration;
import svenz.remote.device.ipremote.AbstractCoded;
import svenz.remote.device.ipremote.AbstractDevice;
import svenz.remote.device.ipremote.ChannelDeviceListener;
import svenz.remote.device.ipremote.PoweredImpl;
import svenz.remote.device.ipremote.QueryChangeListener;
import svenz.remote.device.ipremote.SelectableImpl;
import svenz.remote.device.ipremote.SoundImpl;
import svenz.remote.net.protocol.ssdp.MatchingDeviceListener;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;

/**
 * @author Sven Zethelius
 *
 */
public class ReceiverVSX1123Device extends AbstractDevice
{

	private final Zone1 m_zone1;
	private final Zone2 m_zone2;
	private final ZoneHDMI m_zoneHDMI;
	private final AbstractCoded<String> m_display = new AbstractCoded<String>("display", this);

	public ReceiverVSX1123Device()
	{
		super();

		Configuration codes = getConfiguration();
		Configuration inputs = getConfiguration(getClass().getSimpleName() + "-Inputs.properties");

		m_zone1 = initZone(codes.subset("zone1"), inputs, new Zone1());
		m_zone2 = initZone(codes.subset("zone2"), inputs, new Zone2());
		m_zoneHDMI = initZone(codes.subset("zoneHDMI"), inputs, new ZoneHDMI());

		m_display.setName("AVR.display");
		m_display.setCodes(codes.subset("display"));
		register(m_display);
	}

	public Zone1 getZone1()
	{
		return m_zone1;
	}

	public Zone2 getZone2()
	{
		return m_zone2;
	}

	public ZoneHDMI getZoneHDMI()
	{
		return m_zoneHDMI;
	}

	public String getDisplay()
	{
		String s = m_display.getStatus();
		return s != null ? s : "";
	}
	// TODO change listener

	@Override
	protected IDeviceListener<?> initDeviceListener(ChannelDeviceListener listener)
	{
		listener.setElementName(new QName("http://www.pioneerelectronics.com/xmlns/av", "X_ipRemoteTcpPort"));

		MatchingDeviceListener<?> mlistener = new MatchingDeviceListener<Object>(listener);
		mlistener.setDeviceType("urn:schemas-upnp-org:device:MediaRenderer:1");
		mlistener.setFriendlyName("VSX-1123");
		mlistener.setModelName("VSX-1123/CUXESM");

		return mlistener;
	}

	private <T extends Zone> T initZone(Configuration codes, Configuration inputs, T zone)
	{
		zone.init(codes, inputs);
		return zone;
	}

	private void setStatus(PoweredImpl powered, String status)
	{
		powered.setStatus("0".equals(status));
	}

	private void setVolumeStatus(SoundImpl sound, String vol)
	{
		sound.setVolumeStatus(Integer.parseInt(vol, 10));
	}

	private void setMuteStatus(SoundImpl sound, String mute)
	{
		sound.setMuteStatus("0".equals(mute));
	}

	private void setInputStatus(SelectableImpl select, String input)
	{
		select.setStatus(input);
	}

	private void setDisplay(String display)
	{
		display = display.trim();

		String displayOld = m_display.getStatus();

		// find index of displayOld suffix in display
		if (displayOld != null && displayOld.isEmpty() && display.length() > 2
				&& display.length() < displayOld.length())
		{
			if (displayOld.contains(display))
				return; // do nothing.

			int idx = displayOld.indexOf(display.substring(0, display.length() - 2));
			display = displayOld.substring(0, idx) + display;
		}

		m_display.setStatus(display);
		getLogger().debug("Display:" + display);
	}

	private String getFromHex(String s)
	{
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i += 2)
		{
			sb.append((char) Integer.parseInt(s.substring(i, i + 2), 16));
		}
		return sb.toString();
	}

	@Override
	protected void handleResponse(String response)
	{
		if(response.startsWith("PWR"))
			setStatus(m_zone1.getPowered(), response.substring(3));
		else if (response.startsWith("APR"))
			setStatus(m_zone2.getPowered(), response.substring(3));
		else if (response.startsWith("ZEP"))
			setStatus(m_zoneHDMI.getPowered(), response.substring(3));
		else if (response.startsWith("VOL"))
			setVolumeStatus(m_zone1.m_sound, response.substring(3));
		else if (response.startsWith("ZV"))
			setVolumeStatus(m_zone2.m_sound, response.substring(2));
		else if (response.startsWith("MUT"))
			setMuteStatus(m_zone1.m_sound, response.substring(3));
		else if (response.startsWith("Z2MUT"))
			setMuteStatus(m_zone2.m_sound, response.substring(5));
		else if (response.startsWith("FN"))
			setInputStatus(m_zone1.getInput(), response.substring(2));
		else if (response.startsWith("Z2F"))
			setInputStatus(m_zone2.getInput(), response.substring(3));
		else if (response.startsWith("ZEA"))
			setInputStatus(m_zoneHDMI.getInput(), response.substring(3));
		else if (response.startsWith("FL"))
			setDisplay(getFromHex(response.substring(2)).trim());
		else if (response.startsWith("SR"))
			m_zone1.getListeningMode().setStatus(response.substring(2));
		else if (response.startsWith("LM"))
			// Listening Mode has one set of keys for ?S and SR, but another for ?L and LM.
			getLogger().debug("Listening mode:{}", response);
		else if (response.startsWith("AU"))
			getLogger().debug("Audio Parameter Prohibition:{}", response); // TODO
		else if (response.startsWith("VTA"))
			getLogger().debug("Video Parameter Prohibition:{}", response); // TODO
		else if ("E02".equals(response) && !m_zone1.getPowered().isPowered())
			getLogger().debug("Error {} because receiver is not powered"); // expected
		else
			getLogger().warn("Unhandled response for {}: {}", getClass(), response);
		// TODO handle errors 'E'
	}

	public class Zone
	{
		private final PoweredImpl m_powered = new PoweredImpl();
		private final SelectableImpl m_input = new SelectableImpl();

		public PoweredImpl getPowered()
		{
			return m_powered;
		}

		public SelectableImpl getInput()
		{
			return m_input;
		}

		protected void init(Configuration config, Configuration inputs)
		{
			String type = getClass().getSimpleName().toLowerCase();
			String prefix = "AVR." + type;
			m_powered.setName(prefix + ".powered");
			m_powered.setCodes(config.subset("power"));

			m_input.setName(prefix + ".input");
			initSelectable(m_input, config.subset("input"), inputs);

			m_powered.addChangeListener(new QueryChangeListener(m_input));
			register(m_powered, m_input);
		}
	}

	public class Zone1 extends Zone
	{
		private final SelectableImpl m_listeningMode = new SelectableImpl();
		private final SoundImpl m_sound = new SoundImpl();

		public SoundImpl getSound()
		{
			return m_sound;
		}

		public SelectableImpl getListeningMode()
		{
			return m_listeningMode;
		}

		@Override
		protected void init(Configuration config, Configuration inputs)
		{
			Configuration listeningModes =
					getConfiguration(ReceiverVSX1123Device.class.getSimpleName() + "-ListeningModes.properties");

			m_sound.setName("AVR.zone1.sound");
			m_sound.setCodes(config.subset("sound"));

			m_listeningMode.setName("AVR.zone1.listening");
			initSelectable(m_listeningMode, config.subset("listening"), listeningModes);

			super.init(config, inputs);
			getPowered().addChangeListener(new QueryChangeListener(m_sound, m_listeningMode));
			register(m_sound, m_listeningMode);
		}
	}

	public class Zone2 extends Zone
	{
		private final SoundImpl m_sound = new SoundImpl();

		public SoundImpl getSound()
		{
			return m_sound;
		}

		@Override
		protected void init(Configuration config, Configuration inputs)
		{
			m_sound.setName("AVR.zone2.sound");
			m_sound.setCodes(config.subset("sound"));
			super.init(config, inputs);
			getPowered().addChangeListener(new QueryChangeListener(m_sound));
			register(m_sound);
		}
	}

	public class ZoneHDMI extends Zone
	{

	}
}
