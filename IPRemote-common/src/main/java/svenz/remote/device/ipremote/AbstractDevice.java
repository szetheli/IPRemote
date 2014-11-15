/**
 * 
 */
package svenz.remote.device.ipremote;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.functors.NOPTransformer;
import org.apache.commons.configuration.AbstractFileConfiguration;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.SimplePropertiesConfiguration;
import svenz.remote.common.utilities.Utilities;
import svenz.remote.device.IChangable.IChangeListener;
import svenz.remote.net.nio.ITCPSocketChannelCallback;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.nio.TCPSocketChannelInstance;
import svenz.remote.net.protocol.ssdp.SSDPManager;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;

/**
 * @author Sven Zethelius
 *
 */
public abstract class AbstractDevice implements Closeable
{
	private transient Logger m_logger = LoggerFactory.getLogger(getClass());
	private transient ChannelDeviceListener m_deviceListener = new ChannelDeviceListener();
	private transient SSDPManager m_ssdpManager;
	private transient IDeviceListener<?> m_registeredDeviceListener;
	private final List<AbstractCoded<?>> m_codeds = new ArrayList<AbstractCoded<?>>(6);

	public AbstractDevice()
	{
		m_deviceListener.setChannelCallback(new TCPSocketChannelCallback());
		m_deviceListener.addChangeListener(new InstanceChangeListener());
	}

	protected abstract IDeviceListener<?> initDeviceListener(ChannelDeviceListener listener);

	protected void register(AbstractCoded<?>... codeds)
	{
		Collections.addAll(m_codeds, codeds);
	}

	protected Logger getLogger()
	{
		return m_logger;
	}

	protected WritableByteChannel getWriteChannel()
	{
		return m_deviceListener.getInstance();
	}

	/**
	 * @return true if an instance of this device was found on the network.
	 */
	public boolean isFound()
	{
		return null != m_deviceListener.getAddress();
	}

	/**
	 * @return true if there is a connection to this device
	 */
	public boolean isConnected()
	{
		WritableByteChannel instance = m_deviceListener.getInstance();
		return instance != null && instance.isOpen();
	}

	public void open()
	{
		if (m_registeredDeviceListener == null)
			m_ssdpManager.addDeviceListener(m_registeredDeviceListener = initDeviceListener(m_deviceListener));
	}

	@Override
	public void close() throws IOException
	{
		Utilities.safeClose(m_deviceListener);
		synchronized (this)
		{
			m_ssdpManager.removeDeviceListener(m_registeredDeviceListener);
			m_registeredDeviceListener = null;
		}
	}
	
	protected Configuration getConfiguration()
	{
		return getConfiguration(getClass().getSimpleName() + "-codes.properties");
	}

	protected Configuration getConfiguration(String file)
	{

		AbstractFileConfiguration config = new SimplePropertiesConfiguration();
		Class<?> clazz = getClass();
		InputStream is = null;
		try
		{
			is = clazz.getResourceAsStream(file);
			if (is == null)
				throw new FileNotFoundException(file);
			config.load(new BufferedInputStream(is));
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Unable to load " + file, e);
		}
		finally
		{
			Utilities.safeClose(is);
		}
		config.setThrowExceptionOnMissing(true);
		config.setListDelimiter(',');
		return config;
	}

	public void setChannelManager(SocketChannelManager manager)
	{
		m_deviceListener.setChannelManager(manager);
	}

	public void setSSDPManager(SSDPManager manager)
	{
		m_ssdpManager = manager;
		m_deviceListener.setSSDPManager(manager);
	}

	public void setExecutor(ScheduledExecutorService executor)
	{
		// do nothing
	}

	protected abstract void handleResponse(String response);
	
	@SuppressWarnings("unchecked")
	protected static void initSelectable(SelectableImpl selectable, Configuration config, Configuration inputs)
	{
		selectable.setCodes(config);
		selectable.setOptions(CollectionUtils.collect(inputs.getKeys(), NOPTransformer.INSTANCE));
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append('(').append(getWriteChannel()).append(')')
				.append('\n');
		for (AbstractCoded<?> coded : m_codeds)
			sb.append("  ").append(coded).append('\n');

		return sb.toString();
	}

	/**
	 * Listen for {@link ChannelDeviceListener} changes to update the writable channel the {@link AbstractCoded} use.
	 * 
	 * @author Sven Zethelius
	 * 
	 */
	private class InstanceChangeListener implements IChangeListener
	{
		@Override
		public void stateChanged(Object target, String property)
		{
			if (ChannelDeviceListener.NOTIFY_INSTANCE.equals(property))
			{
				WritableByteChannel writeChannel = getWriteChannel();
				for (final AbstractCoded<?> coded : m_codeds)
				{
					coded.setWriter(writeChannel);
				}
			}
		}

	}
	private class TCPSocketChannelCallback implements ITCPSocketChannelCallback
	{

		/**
		 * normalize the response for calling handeResponse
		 */
		@Override
		public void handleRead(byte[] b, InetSocketAddress address) throws IOException
		{
			// TODO Do we need to deal with message fragmentation? In theory this is all local link, so no.
			String response = new String(b);
			if (getLogger().isDebugEnabled())
				getLogger().debug("Received response from {}: \n{}", getWriteChannel(), response);
			for (String responseLine : response.replaceAll("\r\n", "\n").split("[\n|\r]"))
			{
				if (!"".equals(responseLine))
				{
					try
					{
						handleResponse(responseLine);
					}
					catch (RuntimeException e)
					{
						getLogger().error("Unable to handle response {}", responseLine, e);
					}
				}
			}
		}

		@Override
		public void close() throws IOException
		{
			// lost connection means we need to reset current state
			for (AbstractCoded<?> coded : m_codeds)
				coded.setStatus(null);
		}

		@Override
		public void connectionOpen(TCPSocketChannelInstance instance)
		{
			getLogger().info("Connection opened to {} {}", AbstractDevice.this.getClass().getSimpleName(), instance);
		}

		@Override
		public void connectionClose(TCPSocketChannelInstance instance)
		{
			getLogger().info("Connection closed to {} {}", AbstractDevice.this.getClass().getSimpleName(), instance);
		}

		@Override
		public void connectionFailed(TCPSocketChannelInstance instance)
		{
			getLogger().info("Connection failed to {} {}", AbstractDevice.this.getClass().getSimpleName(), instance);

		}
	}
}
