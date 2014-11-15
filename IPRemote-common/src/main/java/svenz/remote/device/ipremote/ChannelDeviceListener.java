/**
 * 
 */
package svenz.remote.device.ipremote;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.Utilities;
import svenz.remote.device.IChangable;
import svenz.remote.device.impl.ChangableImpl;
import svenz.remote.net.nio.CompoundTCPSocketChannelCallback;
import svenz.remote.net.nio.ITCPSocketChannelCallback;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.protocol.ssdp.SSDPChannelCallback;
import svenz.remote.net.protocol.ssdp.SSDPManager;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;
import svenz.remote.net.protocol.ssdp.jaxb.Device;

/**
 * @author Sven Zethelius
 *
 */
public class ChannelDeviceListener implements IDeviceListener<Object>, IChangable
{
	/**
	 * {@link IChangeListener#stateChanged(Object, String)} called when connected instance changes
	 */
	public static final String NOTIFY_INSTANCE = "instance";

	/**
	 * {@link IChangeListener#stateChanged(Object, String)} called when address changes
	 */
	public static final String NOTIFY_ADDRESS = "address";
	private static final Logger LOGGER = LoggerFactory.getLogger(ChannelDeviceListener.class);
	private final ChangableImpl<Void> m_change = new ChangableImpl<Void>(this, null);
	private SocketChannelManager m_channelManager;
	private SSDPManager m_ssdpManager;
	private IPortReader m_portReader;
	private ITCPSocketChannelCallback m_channelCallback;
	private InetSocketAddress m_address;
	private Device m_device;
	private WritableByteChannel m_instance;
	
	public void setChannelCallback(ITCPSocketChannelCallback channelCallback)
	{
		m_channelCallback = channelCallback;
	}

	public void setChannelManager(SocketChannelManager channelManager)
	{
		m_channelManager = channelManager;
	}

	public void setPort(int port)
	{
		m_portReader = new FixedPortReader(port);
	}

	public void setElementName(QName qname)
	{ // TODO change API to match not using xsd:any
		m_portReader = new ElementPortReader();
	}

	public void setSSDPManager(SSDPManager ssdpManager)
	{
		m_ssdpManager = ssdpManager;
	}

	public SSDPManager getSSDPManager()
	{
		return m_ssdpManager;
	}

	public WritableByteChannel getInstance()
	{
		return m_instance;
	}

	public InetSocketAddress getAddress()
	{
		return m_address;
	}

	public void connect()
	{
		if (m_address == null)
			throw new IllegalStateException("No address to connect to");
		
		ITCPSocketChannelCallback callback =
				new CompoundTCPSocketChannelCallback(Arrays.asList(
						new SSDPChannelCallback(m_ssdpManager, m_device.getUDN()), m_channelCallback));
		
		try
		{
			m_instance = m_channelManager.connect(m_address, callback);
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("Connected to {} @ {}", m_device.getFriendlyName(), m_address);
			m_change.notify(NOTIFY_INSTANCE);
		}
		catch (IOException e)
		{
			LOGGER.error("Unable to connect to device {} {}", new Object[] { m_device.getFriendlyName(),
					m_address, e });
		}

	}

	@Override
	public Object deviceAdded(Device device, InetSocketAddress remoteAddress)
	{
		Integer port = m_portReader.getPort(device, remoteAddress);
		if (port == null)
			return null;

		if (m_address != null)
			throw new IllegalStateException("Instance already registered");

		m_address = new InetSocketAddress(remoteAddress.getAddress(), port);
		m_change.notify(NOTIFY_ADDRESS);

		m_device = device;
		connect();
		return this;
	}

	@Override
	public void deviceRemoved(Object handle)
	{
		m_address = null;
		m_change.notify(NOTIFY_ADDRESS);
		Utilities.safeClose(this);
	}
	
	@Override
	public void close() throws IOException
	{
		synchronized (this)
		{
			Utilities.safeClose(m_instance);
			m_instance = null;
		}
		m_change.notify(NOTIFY_INSTANCE);
	}

	@Override
	public void addChangeListener(IChangeListener listener)
	{
		m_change.addChangeListener(listener);
	}

	@Override
	public void removeChangeListener(IChangeListener listener)
	{
		m_change.removeChangeListener(listener);
	}

	private interface IPortReader
	{
		Integer getPort(Device d, InetSocketAddress remoteAddress);
	}

	private static class FixedPortReader implements IPortReader
	{
		private final int m_port;

		public FixedPortReader(int port)
		{
			super();
			m_port = port;
		}

		@Override
		public Integer getPort(Device d, InetSocketAddress remoteAddress)
		{
			return m_port;
		}
	}

	private static class ElementPortReader implements IPortReader
	{
		@Override
		public Integer getPort(Device d, InetSocketAddress remoteAddress)
		{
			return d.getIPPort();
		}
	}
}
