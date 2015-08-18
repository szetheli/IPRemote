/**
 * 
 */
package svenz.remote.swing;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.LoggingRunnable;
import svenz.remote.common.utilities.Utilities;
import svenz.remote.device.DeviceGroupRegistry;
import svenz.remote.device.impl.pioneer.PlayerBDP150Device;
import svenz.remote.device.impl.pioneer.ReceiverVSX1123Device;
import svenz.remote.device.impl.sharp.TVAquas60LE650Device;
import svenz.remote.device.jaxb.DeviceGroups;
import svenz.remote.net.nio.DatagramListenerChannelInstance;
import svenz.remote.net.nio.ISocketChannelCallback;
import svenz.remote.net.nio.ITCPSocketChannelCallback;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.protocol.ssdp.SSDPManager;

/**
 * @author Sven Zethelius
 *
 */
public class MainDeviceTests
{

	private static final Logger LOGGER = LoggerFactory.getLogger(MainDeviceTests.class);
	private static final String CLASSPATH_ROOT = "file:"
			+ new File(MainDeviceTests.class.getClassLoader().getResource(".").getFile()).toString().replaceAll("\\\\",
					"/");

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception
	{
		Main main = new Main();
		DeviceGroupRegistry deviceRegistry = new DeviceGroupRegistry();
		LocalChannelManager channelManager = new LocalChannelManager();
		SSDPManager ssdpManager = new SSDPManager();
		ResourceBundle resources =
				ResourceBundle.getBundle(Main.class.getPackage().getName() + ".Resources", Locale.getDefault());

		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);
		Utilities.configure(executor, 1, TimeUnit.MINUTES);

		channelManager.setExecutor(executor);

		ssdpManager.setChannelManager(channelManager);
		ssdpManager.setExecutor(executor);

		deviceRegistry.setChannelManager(channelManager);
		deviceRegistry.setExecutor(executor);
		deviceRegistry.setSSDPManager(ssdpManager);

		InputStream is = Main.class.getResourceAsStream("DeviceGroup.xml");
		try
		{
			deviceRegistry.load(new Persister().read(DeviceGroups.class, is));
		}
		finally
		{
			Utilities.safeClose(is);
		}

		PropertiesConfiguration orderConfig = new PropertiesConfiguration();
		// TODO initialize order config

		main.setExecutor(executor);
		main.setDeviceRegistry(deviceRegistry);
		main.setExecutor(executor);
		main.setResources(resources);
		main.setOrderConfiguration(orderConfig);

		Map<String, ICodeResponder> channels = new HashMap<String, ICodeResponder>();
		channels.put("tv", 
				channelManager.addChannel(new InetSocketAddress("192.168.1.14", 10002), TVAquas60LE650Device.class));
		channels.put("bd", 
				channelManager.addChannel(new InetSocketAddress("192.168.1.12", 8102), PlayerBDP150Device.class));
		channels.put("avr",
				channelManager.addChannel(new InetSocketAddress("192.168.1.13", 8102), ReceiverVSX1123Device.class));
		channels.put("ssdp", channelManager);

		Thread t = new Thread(new LoggingRunnable(new InputRunnable(channels, deviceRegistry)));
		t.setDaemon(true);
		t.setName(InputRunnable.class.getSimpleName());
		t.start();
		channelManager.open();
		ssdpManager.open();
		deviceRegistry.open();
		main.show();
	}

	private static class InputRunnable implements Runnable
	{
		private final Map<String, ICodeResponder> m_responders;
		private final DeviceGroupRegistry m_deviceRegistry;

		public InputRunnable(Map<String, ICodeResponder> responders, DeviceGroupRegistry deviceRegistry)
		{
			m_responders = responders;
			m_deviceRegistry = deviceRegistry;
		}

		@Override
		public void run()
		{
			BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));

			try
			{
				for (String line = null; null != (line = rdr.readLine());)
				{
					if ("".equals(line))
						continue;
					if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line))
						return;
					String[] split;
					if ("status".equals(line))
					{
						LOGGER.info("Status:\n{}", m_deviceRegistry);
					}
					else if ((split = line.split("[ \\.]", 2)).length == 2)
					{
						ICodeResponder responder = m_responders.get(split[0].toLowerCase());
						if (responder == null)
							throw new IllegalArgumentException("No handler found for " + line);
						String s = split[1].replaceAll("\\\\r", "\r")
										.replaceAll("\\\\n", "\n");
						responder.respond(s);
					}
					else
					{
						LOGGER.error("Unable to interpret command {}", line);
					}
				}
			}
			catch (IOException e)
			{
				LOGGER.error("Exception from input thread", e);
			}
			finally
			{
				Frame[] frames = Frame.getFrames();
				for (Frame frame : frames)
					frame.dispose();
			}
		}
	}

	private static class LocalChannelManager extends SocketChannelManager implements ICodeResponder
	{
		private final Map<InetSocketAddress, LocalChannel> m_channels = new ConcurrentHashMap<>();
		private final Collection<LocalDatagramChannelInstance> m_datagramChannels =
				new ConcurrentLinkedQueue<LocalDatagramChannelInstance>();
		private final Map<String, String> m_propertyToCode = new HashMap<String, String>();

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void open() throws IOException
		{
			Properties props = new Properties();
			try (InputStream is = getClass().getResourceAsStream("ssdp-responses.properties"))
			{
				props.load(is);
				m_propertyToCode.putAll((Map) props);
			}
		}


		@Override
		public WritableByteChannel connect(InetSocketAddress address, ITCPSocketChannelCallback callback)
				throws IOException
		{
			return connect(address, 0, callback);
		}

		public LocalChannel addChannel(InetSocketAddress address, Class<?> clazz) throws IOException
		{
			LocalChannel channel = new LocalChannel(this);
			channel.setRemoteAddress(address);
			channel.loadCodes(clazz);
			m_channels.put(address, channel);
			return channel;
		}

		@Override
		public WritableByteChannel connect(InetSocketAddress address, int timeout, ITCPSocketChannelCallback callback)
				throws IOException
		{
			LocalChannel channel = m_channels.get(address);
			if (channel == null)
				throw new IllegalStateException("No channel setup for " + address);
			channel.setCallback(callback);
			return channel;
		}
		@Override
		public DatagramListenerChannelInstance listenMultigram(InetSocketAddress address,
				Collection<NetworkInterface> interfaces, ISocketChannelCallback callback) throws IOException
		{
			LocalDatagramChannelInstance instance = new LocalDatagramChannelInstance(this, callback);
			m_datagramChannels.add(instance);
			return instance;
		}

		@Override
		public DatagramListenerChannelInstance listenDatagram(InetSocketAddress address, ISocketChannelCallback callback)
				throws IOException
		{
			LocalDatagramChannelInstance instance = new LocalDatagramChannelInstance(this, callback);
			m_datagramChannels.add(instance);
			return instance;
		}

		@Override
		public void respond(String response) throws IOException
		{
			String s = m_propertyToCode.get(response);
			if (s == null)
				s = response;

			m_datagramChannels.iterator().next().respond(s);
		}
	}

	private static interface ICodeResponder
	{
		public void respond(String response) throws IOException;
	}

	private static class LocalChannel implements WritableByteChannel, ICodeResponder
	{
		private final LocalChannelManager m_channelManager;
		private final Map<String, String> m_codeToProperty = new HashMap<String, String>();
		private final Map<String, String> m_propertyToCode = new HashMap<String, String>();
		private ITCPSocketChannelCallback m_callback;
		private InetSocketAddress m_remoteAddress;

		public LocalChannel(LocalChannelManager channelManager)
		{
			m_channelManager = channelManager;
		}

		public void setRemoteAddress(InetSocketAddress remoteAddress)
		{
			m_remoteAddress = remoteAddress;
		}

		public void setCallback(ITCPSocketChannelCallback callback)
		{
			m_callback = callback;
		}

		public void loadCodes(Class<?> clazz) throws IOException
		{
			Properties props = new Properties();
			try (InputStream is = clazz.getResourceAsStream(clazz.getSimpleName() + "-codes.properties"))
			{
				props.load(is);
				for (Map.Entry<Object, Object> entry : props.entrySet())
				{
					m_codeToProperty.put(entry.getValue().toString(), entry.getKey().toString());
					m_propertyToCode.put(entry.getKey().toString(), entry.getValue().toString());
				}
			}
		}

		@Override
		public boolean isOpen()
		{
			return m_callback != null;
		}

		@Override
		public void close() throws IOException
		{
			m_channelManager.m_channels.remove(m_remoteAddress);
			Utilities.safeClose(m_callback);
			m_callback = null;
			m_remoteAddress = null;
		}

		@Override
		public int write(ByteBuffer src) throws IOException
		{
			String code = new String(src.array(), src.position(), src.remaining());
			LOGGER.info("Writing: {} {}\n{}", m_remoteAddress, m_codeToProperty.get(code), code);
			return src.remaining();
		}

		@Override
		public void respond(String response) throws IOException
		{
			for (int i = 0; !isOpen() && i < 10; i++)
				Utilities.sleep(50);
			if (!isOpen())
				throw new IllegalStateException(m_remoteAddress + " not open for response " + response);
			String s = m_propertyToCode.get(response);
			if (s == null)
				s = response;

			LOGGER.info("Responding: {} {}\n{}", m_remoteAddress, response, s);
			m_callback.handleRead(s.getBytes(), m_remoteAddress);
		}

		@Override
		public String toString()
		{
			return m_remoteAddress.toString();
		}
	}

	private static class LocalDatagramChannelInstance extends DatagramListenerChannelInstance implements ICodeResponder
	{
		private static final Pattern ADDRESS = Pattern.compile("(.+):(\\d+)");
		private final LocalChannelManager m_channelManager;
		private final ISocketChannelCallback m_callback;

		public LocalDatagramChannelInstance(LocalChannelManager localChannelManager, ISocketChannelCallback callback)
		{
			super(callback, null, new ArrayList<>());
			m_channelManager = localChannelManager;
			m_callback = callback;

		}

		@Override
		public void send(ByteBuffer b, SocketAddress address) throws IOException
		{
			LOGGER.info("Writing: {}\n{}", address, new String(b.array(), b.position(), b.remaining()));
		}

		@Override
		public void respond(String response) throws IOException
		{
			Matcher matcher = ADDRESS.matcher(response);
			if (!matcher.find())
				throw new IllegalStateException("Unable to find address for response\n" + response);
			InetSocketAddress address = new InetSocketAddress(matcher.group(1), Integer.parseInt(matcher.group(2)));
			response = response.substring(matcher.end());
			response = response.replaceAll("classpath:", CLASSPATH_ROOT);

			LOGGER.info("Responding: {}\n{}", address, response);
			m_callback.handleRead(response.getBytes(), address);
		}

		@Override
		public void close()
		{
			m_channelManager.m_datagramChannels.remove(this);
			Utilities.safeClose(m_callback);
		}
	}
}
