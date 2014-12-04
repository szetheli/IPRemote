/**
 *
 * SSDPManager.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.net.protocol.ssdp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import svenz.remote.common.utilities.LoggingRunnable;
import svenz.remote.common.utilities.Utilities;
import svenz.remote.net.nio.AsyncSocketChannelCallback;
import svenz.remote.net.nio.DatagramListenerChannelInstance;
import svenz.remote.net.nio.ISocketChannelCallback;
import svenz.remote.net.nio.ITCPSocketChannelCallback;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.nio.TCPSocketChannelInstance;
import svenz.remote.net.protocol.ssdp.SSDPManager.SSDPPacket.NotifyTypeState;
import svenz.remote.net.protocol.ssdp.SSDPManager.SSDPPacket.PacketType;
import svenz.remote.net.protocol.ssdp.jaxb.Device;
import svenz.remote.net.protocol.ssdp.jaxb.Root;

/**
 * SSDPManager provides an implementation of SSDP
 * 
 * @author Sven Zethelius
 * 
 */
public class SSDPManager implements Closeable
{

	private static final Logger LOGGER = LoggerFactory.getLogger(SSDPManager.class);
	/**
	 * Default destination port for SSDP multicast messages
	 */
	public static final int MULTICAST_PORT = 1900;

	/**
	 * Port to listen to UDP responses
	 */
	public static final int UDP_PORT = 8008;

	/**
	 * Default IPv4 multicast address for SSDP messages
	 */
	public static final String IPV4_ADDRESS = "239.255.255.250";

	public static final String IPV6_LINK_LOCAL_ADDRESS = "FF02::C";
	public static final String IPV6_SUBNET_ADDRESS = "FF03::C";
	public static final String IPV6_ADMINISTRATIVE_ADDRESS = "FF04::C";
	public static final String IPV6_SITE_LOCAL_ADDRESS = "FF05::C";
	public static final String IPV6_GLOBAL_ADDRESS = "FF0E::C";

	private SocketChannelManager m_channelManager;
	private ScheduledExecutorService m_executor;
	private final Collection<Network> m_networks = new ConcurrentLinkedQueue<Network>();
	private final Collection<IPacketListener> m_packetListeners = new ConcurrentLinkedQueue<IPacketListener>();
	private final Collection<IDeviceListener<?>> m_deviceListeners = new ConcurrentLinkedQueue<IDeviceListener<?>>();
	private final Map<String, DeviceTracker> m_devices = new ConcurrentHashMap<String, DeviceTracker>();
	private Future<?> m_searchFuture;
	private long m_searchFrequencyMS;
	private boolean m_opened = false;
	private Collection<String> m_allowedContentTypes = new HashSet<String>(Arrays.asList("text/xml", "text/html", "application/xml"));

	public SSDPManager()
	{
		setSearchFrequency(25, TimeUnit.MINUTES);
		addPacketListener(new DeviceNotifyListener());
	}

	/**
	 * 
	 * 
	 * @param manager
	 *            manager to use for communication. manager should be opened
	 *            independently before calling {@link #open()}
	 * @param executor
	 */
	public SSDPManager(SocketChannelManager manager, ScheduledExecutorService executor)
	{
		this();
		m_channelManager = manager;
		m_executor = executor;
	}

	public void setChannelManager(SocketChannelManager channelManager)
	{
		m_channelManager = channelManager;
	}

	public void setExecutor(ScheduledExecutorService executor)
	{
		m_executor = executor;
	}

	public void setSearchFrequency(long frequency, TimeUnit unit)
	{
		m_searchFrequencyMS = TimeUnit.MILLISECONDS.convert(frequency, unit);
	}

	private void addNetwork(String address) throws SocketException
	{
		Network n = new Network(new InetSocketAddress(address, MULTICAST_PORT));
		if (!n.getInterfaces().isEmpty())
			m_networks.add(n);
	}

	/**
	 * Prepare the SSDP for work.
	 * 
	 */
	public void open()
	{
		synchronized (m_networks)
		{
			m_opened = true;
		}
		m_executor.execute(new LoggingRunnable(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					synchronized (m_networks)
					{
						if (!m_opened)
							return;

						addNetwork(IPV4_ADDRESS);
						addNetwork(IPV6_LINK_LOCAL_ADDRESS);// TODO which IPV6 address to use

						ISocketChannelCallback callback =
								new AsyncSocketChannelCallback(m_executor, new MulticastReadCallback());
						for (Network network : m_networks)
						{
							network.listen(m_channelManager, callback);
						}
						m_searchFuture = m_executor.scheduleAtFixedRate(new LoggingRunnable(new Runnable() {
							@Override
							public void run()
							{
								search(new SSDPSearchRequest());
							}
						}), 0, m_searchFrequencyMS, TimeUnit.MILLISECONDS);
					}
				}
				catch (IOException e)
				{
					LOGGER.error("Unable to connect", e);
					Utilities.safeClose(SSDPManager.this);
				}
			}
		}));
	}

	@Override
	public void close() throws IOException
	{
		synchronized (m_networks)
		{
			if (m_searchFuture != null)
				m_searchFuture.cancel(false);
			m_opened = false;
			for (Network n : m_networks)
				n.close();
			m_networks.clear();
			for (DeviceTracker tracker : m_devices.values())
			{
				tracker.close();
			}
			m_devices.clear();
		}
	}

	public void addPacketListener(IPacketListener listener)
	{
		m_packetListeners.add(listener);
	}

	public void removePacketListener(IPacketListener listener)
	{
		m_packetListeners.remove(listener);
	}

	private void notifyPacket(final SSDPPacket packet, final InetSocketAddress address) throws IOException
	{
		for (final IPacketListener listener : m_packetListeners)
		{
			async(new Runnable()
				{					
					@Override
					public void run()
					{
						listener.handlePacket(packet, address);
					}
				});
		}
	}

	public void addDeviceListener(IDeviceListener<?> listener)
	{
		m_deviceListeners.add(listener);
	}

	public void removeDeviceListener(IDeviceListener<?> listener)
	{
		m_deviceListeners.remove(listener);
		synchronized (m_devices)
		{
			for (DeviceTracker tracker : m_devices.values())
			{
				tracker.removeListener(listener);
			}
		}
	}

	private void async(Runnable r)
	{
		m_executor.execute(new LoggingRunnable(r));
	}

	private static String SEARCH_FORMAT = 
			"M-SEARCH * HTTP/1.1\r\n"+
			"ST: %1$s\r\n"+
			"MX: %2$s\r\n"+
			"MAN: \"ssdp:discover\"\r\n"+
			"HOST: %3$s:%4$d\r\n"+
			"\r\n";
	
	public void search(SSDPSearchRequest request)
	{
		synchronized (m_networks)
		{
			for (Network n : m_networks)
			{
				InetSocketAddress remoteAddress = n.getRemoteAddress();
				String address = remoteAddress.getAddress().getHostAddress();
				String msg =
						String.format(SEARCH_FORMAT, request.getDevice(), request.getMX(), address,
								remoteAddress.getPort());
				LOGGER.trace("Sending to {}: {}", remoteAddress, msg);
				n.send(msg.getBytes());
			}
		}
	}

	private void deviceAdd(SSDPPacket packet, final InetSocketAddress address)
	{
		String deviceId = packet.getUDN();
		long age = packet.getCacheMaxAgeSeconds();

		boolean existing = true;
		DeviceTracker tracker;
		synchronized (m_devices)
		{
			tracker = m_devices.get(deviceId);
			if (tracker == null)
			{
				existing = false;
				m_devices.put(deviceId, tracker = new DeviceTracker());
			}
		}
		tracker.refresh(age);
		if (existing)
		{
			return;
		}

		String location = packet.getLocation();
		InputStream is = null;
		try
		{
			URL url = new URL(location);
			URLConnection conn = url.openConnection();
			is = conn.getInputStream();
			if(LOGGER.isTraceEnabled())
			{
				String s = read(is);
				LOGGER.info("{}\n{}", conn.getHeaderFields(), s);
				is = new ByteArrayInputStream(s.getBytes());
			}
			Device device = getDevice(is, conn);
						
			notifyDeviceListeners(address, device, tracker);
		}
		catch (Exception e)
		{
			LOGGER.error("Unable to handle device add from " + location, e);
		}
		finally
		{
			Utilities.safeClose(is);
		}
	}

	private void notifyDeviceListeners(final InetSocketAddress address, final Device device,
			final DeviceTracker dtracker)
	{
		for (final IDeviceListener<?> listener : m_deviceListeners)
		{
			async(new Runnable()
				{
					@Override
					public void run()
					{
						Object handle = listener.deviceAdded(device, address);
						if (handle != null)
							dtracker.m_handles.put(listener, handle);
					}
				});
		}
	}

	private Device getDevice(InputStream is, URLConnection conn) throws Exception
	{
		String contentType = conn.getContentType();
		String charset = "UTF-8";
		int idx = contentType.indexOf(';');
		if(idx > 0)
		{
			contentType = contentType.substring(0, idx);
			// TODO handle charset
		}
		if (contentType == null || m_allowedContentTypes.contains(contentType))
		{
			Root root = new Persister().read(Root.class, new InputStreamReader(is, charset));
			return root.getDevice();
		}
		else
		{ // liar liar
			// SSDP Location didn't actually point to a valid device location.
			Device d = new Device();
			return d;
		}
	}

	private String read(InputStream is) throws IOException
	{
		BufferedReader rdr = new BufferedReader(new InputStreamReader(is));
		try
		{
			char[] b = new char[8024];
			StringBuilder sb = new StringBuilder();
			int c = 0;
			while (-1 != (c = rdr.read(b)))
			{
				sb.append(b, 0, c);
			}
			return sb.toString();
		}
		finally
		{
			Utilities.safeClose(rdr);
		}
	}

	public void notifyDeviceRemoved(String udn)
	{
		DeviceTracker tracker;
		synchronized (m_devices)
		{
			tracker = m_devices.remove(udn);
		}
		if (tracker != null)
			tracker.run();
	}

	// TODO Listen for Search Request

	// TODO send Notify

	private static class Network implements Closeable
	{
		private final InetSocketAddress m_address;
		private final Collection<NetworkInterface> m_interfaces = new ArrayList<NetworkInterface>();
		private DatagramListenerChannelInstance m_multigramInstance;
		private final Collection<DatagramListenerChannelInstance> m_listeners = 
				new ConcurrentLinkedQueue<DatagramListenerChannelInstance>();

		public Network(InetSocketAddress address) throws SocketException
		{
			m_address = address;
			
			Class<? extends InetAddress> clazz = m_address.getAddress().getClass();
			for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();)
			{
				NetworkInterface inet = e.nextElement();
				if (isIPvX(inet.getInetAddresses(), clazz))
					m_interfaces.add(inet);
			}
		}

		public InetSocketAddress getRemoteAddress()
		{
			return m_address;
		}

		public Collection<NetworkInterface> getInterfaces()
		{
			return m_interfaces;
		}

		/**
		 * Start listening to multicast and UDP ports. Listens to all UDP
		 * addresses individually so that we can send with the same port we
		 * listen to.
		 * 
		 * @param channelManager
		 * @param callback
		 * @throws IOException
		 */
		public void listen(SocketChannelManager channelManager, ISocketChannelCallback callback) throws IOException
		{
			m_multigramInstance =
					channelManager.listenMultigram(m_address, m_interfaces, callback);
			LOGGER.debug("Multicast listening on {}", m_multigramInstance);

			Class<?> addressClazz = m_address.getAddress().getClass();
			for (NetworkInterface inet : m_interfaces)
			{
				for (Enumeration<InetAddress> e = inet.getInetAddresses(); e.hasMoreElements();)
				{
					InetAddress address = e.nextElement();
					if (!isIPvX(address, addressClazz))
						continue;
					DatagramListenerChannelInstance datagram =
							channelManager.listenDatagram(new InetSocketAddress(address, UDP_PORT), callback);
					m_listeners.add(datagram);
					LOGGER.debug("Datagram listening on {}", datagram);
				}
			}
		}

		public void send(byte[] b)
		{
			for (Iterator<DatagramListenerChannelInstance> iter = m_listeners.iterator(); iter.hasNext(); )
			{
				DatagramListenerChannelInstance instance = iter.next();
				try
				{
					instance.send(ByteBuffer.wrap(b), m_address);
				}
				catch (NoRouteToHostException e)
				{
					LOGGER.debug("Unable to send to unroutable {}", instance);
					iter.remove();
					Utilities.safeClose(instance);
				}
				catch (IOException e)
				{
					LOGGER.error("Unable to send to {}", instance, e);
				}
			}
			if(m_listeners.isEmpty())
			{
				throw new IllegalStateException("Unable to send due to no networks available");
			}
		}

		private boolean isIPvX(InetAddress address, Class<?> clazz)
		{
			return clazz.isInstance(address) && !address.isLoopbackAddress();
		}

		private boolean isIPvX(Enumeration<InetAddress> addresses, Class<? extends InetAddress> clazz)
		{
			while (addresses.hasMoreElements())
			{
				InetAddress address = addresses.nextElement();
				if (isIPvX(address, clazz))
					return true;
			}
			return false;
		}

		@Override
		public void close() throws IOException
		{
			Utilities.safeClose(m_multigramInstance);
			for (Iterator<DatagramListenerChannelInstance> iter = m_listeners.iterator(); iter.hasNext();)
			{
				Utilities.safeClose(iter.next());
				iter.remove();
			}
		}

	}

	public static class SSDPSearchRequest
	{
		private final String m_device;
		private final int m_mx;

		public SSDPSearchRequest()
		{
			this("upnp:rootdevice", 3);
		}

		public SSDPSearchRequest(String device, int mx)
		{
			m_device = device;
			m_mx = mx;
		}

		public String getDevice()
		{
			return m_device;
		}

		public int getMX()
		{
			return m_mx;
		}
	}

	private static final Pattern HEADER_PARSE = Pattern.compile("(.+?)\\s*:\\s*(.*)");;
	private static final Pattern USN_PARSE = Pattern.compile("::");
	private static final Pattern CACHE_PARSE = Pattern.compile("max-age=\\s*(\\d+)");
	public static class SSDPPacket
	{
		public enum PacketType
		{
			Search, SearchResponse, Notify
		}

		public enum NotifyTypeState
		{
			Alive, ByeBye
		}

		private final Map<String, String> m_headers = new LinkedHashMap<String, String>();

		public static SSDPPacket parse(String msg) throws IOException
		{
			SSDPPacket packet = new SSDPPacket();
			BufferedReader rdr = new BufferedReader(new StringReader(msg));

			try
			{
				packet.init(rdr);
			}
			finally
			{
				Utilities.safeClose(rdr);
			}
			return packet;
		}

		private void init(BufferedReader rdr) throws IOException
		{
			String line = rdr.readLine();
			m_headers.put("", line.toUpperCase());
			while (null != (line = rdr.readLine()))
			{
				if (line.isEmpty())
					continue;
				Matcher matcher = HEADER_PARSE.matcher(line);
				if (!matcher.matches())
					continue; // TODO what to do
				m_headers.put(matcher.group(1).toLowerCase(), matcher.group(2));
			}
		}

		public PacketType getPacketType()
		{
			String header = m_headers.get("");
			if (header.startsWith("NOTIFY "))
				return PacketType.Notify;
			if (header.startsWith("HTTP"))
				return PacketType.SearchResponse;
			if (header.startsWith("M-SEARCH "))
				return PacketType.Search;
			return null;
		}

		public String getLocation()
		{
			return m_headers.get("location");
		}

		public String getNT()
		{
			return m_headers.get("nt");
		}

		public NotifyTypeState getNotifyTypeState()
		{
			String nts = m_headers.get("nts");
			if (nts == null)
				return null;
			if ("ssdp:alive".equals(nts))
				return NotifyTypeState.Alive;
			if ("ssdp:byebye".equals(nts))
				return NotifyTypeState.ByeBye;
			LOGGER.debug("Invalid nts '{}' from packet.", nts);
			return null;
		}

		public String getST()
		{
			return m_headers.get("st");
		}

		public String getUSN()
		{
			return m_headers.get("usn");
		}

		public String getUDN()
		{
			String usn = getUSN();
			if (usn == null)
				return null;
			String[] split = USN_PARSE.split(usn, 2);
			return split[0].trim();
		}

		public long getCacheMaxAgeSeconds()
		{
			String cacheControl = m_headers.get("cache-control");
			if (cacheControl == null)
				return 1800;
			Matcher matcher = CACHE_PARSE.matcher(cacheControl);
			return matcher.matches() ? Long.parseLong(matcher.group(1)) : 1800;
		}
		// TODO more fields
	}

	public static interface IPacketListener extends EventListener, Closeable
	{
		void handlePacket(SSDPPacket packet, InetSocketAddress remoteAddress);
	}
	
	public static interface IDeviceListener<T> extends EventListener, Closeable
	{
		T deviceAdded(Device device, InetSocketAddress remoteAddress);

		void deviceRemoved(T handle);
	}


	private class MulticastReadCallback implements ITCPSocketChannelCallback
	{

		@Override
		public void close() throws IOException
		{
			for (IPacketListener listener : m_packetListeners)
				Utilities.safeClose(listener);
		}

		@Override
		public void handleRead(byte[] b, InetSocketAddress address) throws IOException
		{
			String msg = new String(b, "UTF-8");
			LOGGER.trace("Received from {}:\r\n{}", address, msg);
			SSDPPacket packet = SSDPPacket.parse(msg);

			if (null != packet.getPacketType())
			{
				notifyPacket(packet, address);
			}
			else
			{
				LOGGER.debug("Unhandled message from {}:\r\n{}", address, msg);
			}
		}

		@Override
		public void connectionOpen(TCPSocketChannelInstance instance)
		{
			throw new UnsupportedOperationException("connectionOpen");
		}

		@Override
		public void connectionClose(TCPSocketChannelInstance instance)
		{
			throw new UnsupportedOperationException("connectionClose");
		}

		@Override
		public void connectionFailed(TCPSocketChannelInstance instance)
		{
			throw new UnsupportedOperationException("connectionFailed");
		}
	}

	private class DeviceTracker implements Runnable, Closeable
	{
		private Future<?> m_future;
		private final Map<IDeviceListener<?>, Object> m_handles =
				new ConcurrentHashMap<SSDPManager.IDeviceListener<?>, Object>();

		@SuppressWarnings("rawtypes")
		@Override
		public synchronized void run()
		{
			close();
			for (Map.Entry<IDeviceListener<?>, Object> entry : m_handles.entrySet())
			{
				final Object handle = entry.getValue();
				final IDeviceListener listener = entry.getKey();
				async(new Runnable() {
					@SuppressWarnings("unchecked")
					@Override
					public void run()
					{
						listener.deviceRemoved(handle);
					}
				});
			}
		};

		public synchronized void removeListener(IDeviceListener<?> listener)
		{
			m_handles.remove(listener);
		}

		public synchronized void refresh(long ageSeconds)
		{
			if (m_future != null)
				m_future.cancel(false);
			m_future = m_executor.schedule(this, ageSeconds, TimeUnit.SECONDS);
		}

		@Override
		public synchronized void close()
		{
			if (m_future != null)
				m_future.cancel(false);
		}
	}

	private class DeviceNotifyListener implements IPacketListener
	{
		
		@Override
		public void handlePacket(SSDPPacket packet, InetSocketAddress address)
		{
			if (m_deviceListeners.isEmpty())
				return;

			PacketType packetType = packet.getPacketType();
			String nt;
			NotifyTypeState nts;
			switch(packetType)
			{
			case Notify:
				nt = packet.getNT();
				nts = packet.getNotifyTypeState();
				break;
			case Search:
				// TODO reply to search? Use search as trigger to search if we haven't seen that IP?
				return;
			case SearchResponse:
				nt = packet.getST();
				nts = NotifyTypeState.Alive;
				break;
			default:
				LOGGER.warn("Unknown PacketType {}", packetType);
				return;
			}
			
			if (!"upnp:rootdevice".equals(nt))
				return; // ignore non-root device notifications
			if (NotifyTypeState.Alive.equals(nts))
				deviceAdd(packet, address);
			else if (NotifyTypeState.ByeBye.equals(nts))
				notifyDeviceRemoved(packet.getUDN());
			else
				LOGGER.warn("Unknown nts {}", nts);
			return;
		}

		@Override
		public void close() throws IOException
		{
			for (IDeviceListener<?> listener : m_deviceListeners)
				Utilities.safeClose(listener);
		}
	}

}
