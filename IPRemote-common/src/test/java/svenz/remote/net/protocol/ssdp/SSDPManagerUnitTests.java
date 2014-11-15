/**
 * 
 */
package svenz.remote.net.protocol.ssdp;

import static org.hamcrest.core.CombinableMatcher.either;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static svenz.test.helper.TestHelper.waitCapture;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.net.nio.DatagramListenerChannelInstance;
import svenz.remote.net.nio.ISocketChannelCallback;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;
import svenz.remote.net.protocol.ssdp.SSDPManager.IPacketListener;
import svenz.remote.net.protocol.ssdp.SSDPManager.SSDPPacket;
import svenz.remote.net.protocol.ssdp.SSDPManager.SSDPPacket.PacketType;
import svenz.remote.net.protocol.ssdp.jaxb.Device;
import svenz.test.helper.CaptureAppender;
import svenz.test.helper.PropertyMatcher;
import svenz.test.helper.TestHelper;
/**
 * @author Sven Zethelius
 *
 */
@SuppressWarnings("unchecked")
public class SSDPManagerUnitTests
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SSDPManagerUnitTests.class);
	private static final String SEARCH_MESSAGE = "M-SEARCH * HTTP/1.1\r\n"
			+ "ST: upnp:rootdevice\r\n" 
			+ "MX: 3\r\n" 
			+ "MAN: \"ssdp:discover\"\r\n"  
			+ "HOST: 239.255.255.250:1900\r\n\r\n";

	private final IMocksControl m_control = EasyMock.createControl();
	private final SocketChannelManager m_channelManager = m_control.createMock("SocketChannelManager", SocketChannelManager.class);
	private final DatagramListenerChannelInstance m_multigramInstance = m_control.createMock("MultigramInstance", DatagramListenerChannelInstance.class);
	private final DatagramListenerChannelInstance m_datagramInstance = m_control.createMock("DatagramInstance", DatagramListenerChannelInstance.class);

	private final ScheduledThreadPoolExecutor m_executor = new ScheduledThreadPoolExecutor(4);
	private SSDPManager m_manager;

	private final CaptureAppender m_appender = CaptureAppender.install(getClass().getPackage().getName());

	private final Capture<InetSocketAddress> m_captureMCast = new Capture<>(CaptureType.ALL);
	private final Capture<InetSocketAddress> m_captureUDP = new Capture<>(CaptureType.ALL);
	private final Capture<ISocketChannelCallback> m_captureCallback = new Capture<>(CaptureType.ALL);

	@Rule
	public TestName m_name = new TestName();

	@Before
	public void setup() throws Exception
	{
		m_appender.setThreshold(Level.ERROR);
		LOGGER.info("Starting {}", m_name.getMethodName());
		TestHelper.setLogger(getClass().getPackage().getName(), Level.TRACE);

		m_executor.prestartAllCoreThreads();
		m_control.replay();
		m_manager = new SSDPManager(m_channelManager, m_executor);
		m_control.verify();
		m_control.reset();
	}

	@After
	public void teardown() throws IOException
	{
		expectClose(m_multigramInstance, m_captureMCast);
		expectClose(m_datagramInstance, m_captureUDP);

		m_control.replay();
		m_manager.close();
		m_control.verify();
		TestHelper.resetLoggers();

		assertEquals(Collections.EMPTY_LIST, m_appender.getEvents());
		CaptureAppender.reset();
		LOGGER.info("Ending {}", m_name.getMethodName());

	}

	@Test
	public void testOpen() throws Exception
	{
		Capture<Collection<NetworkInterface>> captureNetworks = new Capture<>(CaptureType.ALL);
		Capture<InetSocketAddress> captureMCastSend = new Capture<>(CaptureType.ALL);
		Capture<ByteBuffer> captureMCastBytes = new Capture<>(CaptureType.ALL);

		EasyMock.expect(m_channelManager.listenMultigram(
				EasyMock.capture(m_captureMCast), 
				EasyMock.capture(captureNetworks), 
				EasyMock.capture(m_captureCallback)))
				.andReturn(m_multigramInstance).atLeastOnce();
		
		EasyMock.expect(m_channelManager.listenDatagram(
				EasyMock.capture(m_captureUDP), 
				EasyMock.capture(m_captureCallback)))
				.andReturn(m_datagramInstance).atLeastOnce();
		
		m_datagramInstance.send(EasyMock.capture(captureMCastBytes), EasyMock.capture(captureMCastSend));
		EasyMock.expectLastCall().atLeastOnce();
		
		m_control.replay();
		m_manager.open();
		// Wait for the first interface to be registered
		waitCapture(m_captureMCast, 1);
		Thread.sleep(30); // pause for the rest of the interfaces if any
		// Wait for bytes to be sent on the interfaces
		waitCapture(captureMCastBytes, m_captureMCast.getValues().size());
		Thread.sleep(30); // wait for processing
		m_control.verify();
		m_control.reset();

		assertThat(m_captureMCast.getValues(), 
				either(hasItem(new InetSocketAddress(SSDPManager.IPV4_ADDRESS, SSDPManager.MULTICAST_PORT)))
						.or(hasItem(new InetSocketAddress(SSDPManager.IPV6_LINK_LOCAL_ADDRESS, SSDPManager.MULTICAST_PORT))));

		assertThat(m_captureUDP.getValues(), hasItem(new PropertyMatcher<>(InetSocketAddress.class, "port", SSDPManager.UDP_PORT)));

		assertEquals("Send on all UDP addresses", m_captureUDP.getValues().size(), captureMCastSend.getValues().size());
		assertEquals("Send to all MCast ports listening for", new HashSet<>(m_captureMCast.getValues()), new HashSet<>(captureMCastSend.getValues()));
	}
	
	@Test
	public void testPacketListener() throws Exception
	{
		testOpen();
		IPacketListener listener = m_control.createMock("IPacketListener", IPacketListener.class);
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		Capture<SSDPPacket> capturePacket = new Capture<>(CaptureType.ALL);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);
		String msg = SEARCH_MESSAGE;

		doRegister(listener, null);

		listener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);

		SSDPPacket packet = capturePacket.getValue();
		assertEquals(PacketType.Search, packet.getPacketType());
		assertEquals("upnp:rootdevice", packet.getST());

		capturePacket.getValues().clear();
		m_control.replay();
		m_manager.removePacketListener(listener);
		callback.handleRead(msg.getBytes(), address);
		Thread.sleep(30);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testPacketListenerException() throws Exception
	{
		testOpen();
		IPacketListener listener = m_control.createMock("IPacketListener", IPacketListener.class);
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		Capture<SSDPPacket> capturePacket = new Capture<>(CaptureType.ALL);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);
		String msg = SEARCH_MESSAGE;

		doRegister(listener, null);

		listener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		EasyMock.expectLastCall().andThrow(new NullPointerException("Test exception"));
		doDevicePacket(callback, msg, address, null, capturePacket);

		SSDPPacket packet = capturePacket.getValue();
		assertEquals(PacketType.Search, packet.getPacketType());
		assertEquals("upnp:rootdevice", packet.getST());
		List<LoggingEvent> events = m_appender.getEvents();
		assertTrue(!events.isEmpty());
		assertEquals("Test exception", events.get(0).getThrowableInformation().getThrowable().getMessage());
		events.clear();
	}

	@Test
	public void testDeviceListener() throws Exception
	{
		testOpen();

		IPacketListener plistener = m_control.createMock("IPacketListener", IPacketListener.class);
		IDeviceListener<Object> dlistener = m_control.createMock("IDeviceListener", IDeviceListener.class);
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);

		String msg = getResponseMessage();

		Object o = new Object();
		Capture<Device> captureDevice = new Capture<>(CaptureType.ALL);
		Capture<SSDPPacket> capturePacket = new Capture<>(CaptureType.ALL);

		doRegister(plistener, dlistener);

		EasyMock.expect(dlistener.deviceAdded(EasyMock.capture(captureDevice), EasyMock.eq(address))).andReturn(o);
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, captureDevice, capturePacket);
		
		Device device = captureDevice.getValue();
		assertEquals("Test device", device.getFriendlyName());

		// search response from a different address, since it was MCast to all
		// no second notify
		address = m_captureMCast.getValues().get(m_captureMCast.getValues().size() - 1);
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));

		doDevicePacket(callback, msg, address, null, capturePacket);

		// notify refresh
		// no second notify
		msg = getNotifyMessage(address, "ssdp:alive");
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);

		// notify bye
		msg = getNotifyMessage(address, "ssdp:byebye");
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		dlistener.deviceRemoved(o);
		doDevicePacket(callback, msg, address, null, capturePacket);
	}

	@Test
	public void testDeviceListenerNull() throws Exception
	{
		testOpen();

		IPacketListener plistener = m_control.createMock("IPacketListener", IPacketListener.class);
		IDeviceListener<Object> dlistener = m_control.createMock("IDeviceListener", IDeviceListener.class);
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);

		String msg = getResponseMessage();

		Capture<Device> captureDevice = new Capture<>(CaptureType.ALL);
		Capture<SSDPPacket> capturePacket = new Capture<>(CaptureType.ALL);

		doRegister(plistener, dlistener);

		EasyMock.expect(dlistener.deviceAdded(EasyMock.capture(captureDevice), EasyMock.eq(address))).andReturn(null);
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);

		Device device = captureDevice.getValue();
		assertEquals("Test device", device.getFriendlyName());

		// search response from a different address, since it was MCast to all
		// no second notify
		address = m_captureMCast.getValues().get(m_captureMCast.getValues().size() - 1);
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);

		// notify refresh
		// no second notify
		msg = getNotifyMessage(address, "ssdp:alive");
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);

		// notify bye
		msg = getNotifyMessage(address, "ssdp:byebye");
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);
	}

	@Test
	public void testDeviceListenerException() throws Exception
	{
		testOpen();

		IPacketListener plistener = m_control.createMock("IPacketListener", IPacketListener.class);
		IDeviceListener<Object> dlistener = m_control.createMock("IDeviceListener", IDeviceListener.class);
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);

		String msg = getResponseMessage();

		Capture<Device> captureDevice = new Capture<>(CaptureType.ALL);
		Capture<SSDPPacket> capturePacket = new Capture<>(CaptureType.ALL);

		doRegister(plistener, dlistener);

		EasyMock.expect(dlistener.deviceAdded(EasyMock.capture(captureDevice), EasyMock.eq(address))).andThrow(
				new NullPointerException("Test exception"));
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, captureDevice, capturePacket);

		Device device = captureDevice.getValue();
		assertEquals("Test device", device.getFriendlyName());
		List<LoggingEvent> events = m_appender.getEvents();
		for (int i = 0; i < 100 && events.isEmpty(); i++)
			Thread.sleep(10);
		assertTrue(!events.isEmpty());
		assertEquals("Test exception", events.get(0).getThrowableInformation().getThrowable().getMessage());
		events.clear();

		// search response from a different address, since it was MCast to all
		// no second notify
		address = m_captureMCast.getValues().get(m_captureMCast.getValues().size() - 1);
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);

		// notify refresh
		// no second notify
		msg = getNotifyMessage(address, "ssdp:alive");
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);

		// notify bye
		msg = getNotifyMessage(address, "ssdp:byebye");
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);
	}

	@Test
	public void testDeviceListenerUnregister() throws Exception
	{
		testOpen();

		IPacketListener plistener = m_control.createMock("IPacketListener", IPacketListener.class);
		IDeviceListener<Object> dlistener = m_control.createMock("IDeviceListener", IDeviceListener.class);
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);

		String msg = getResponseMessage();

		Object o = new Object();
		Capture<Device> captureDevice = new Capture<>(CaptureType.ALL);
		Capture<SSDPPacket> capturePacket = new Capture<>(CaptureType.ALL);

		doRegister(plistener, dlistener);

		EasyMock.expect(dlistener.deviceAdded(EasyMock.capture(captureDevice), EasyMock.eq(address))).andReturn(o);
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, captureDevice, capturePacket);

		Device device = captureDevice.getValue();
		assertEquals("Test device", device.getFriendlyName());

		m_control.replay();
		m_manager.removeDeviceListener(dlistener);
		m_control.verify();
		m_control.reset();

		// notify bye
		msg = getNotifyMessage(address, "ssdp:byebye");
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);
	}

	@Test
	public void testDeviceMessageSearch() throws Exception
	{
		testOpen();
		IPacketListener plistener = m_control.createMock("IPacketListener", IPacketListener.class);
		IDeviceListener<Object> dlistener = m_control.createMock("IDeviceListener", IDeviceListener.class);
		Capture<SSDPPacket> capturePacket = new Capture<>(CaptureType.ALL);

		doRegister(plistener, dlistener);

		String msg = SEARCH_MESSAGE;
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);

		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);
	}

	@Test
	public void testDeviceMessageOther() throws Exception
	{
		testOpen();
		IPacketListener plistener = m_control.createMock("IPacketListener", IPacketListener.class);
		IDeviceListener<Object> dlistener = m_control.createMock("IDeviceListener", IDeviceListener.class);

		doRegister(plistener, dlistener);

		String msg = SEARCH_MESSAGE.replace("M-SEARCH", "mSEARCH");
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);

		m_control.replay();
		callback.handleRead(msg.getBytes(), address);
		Thread.sleep(30);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testDeviceMessageByeByeWithoutPreviousStatus() throws Exception
	{
		testOpen();

		IPacketListener plistener = m_control.createMock("IPacketListener", IPacketListener.class);
		IDeviceListener<Object> dlistener = m_control.createMock("IDeviceListener", IDeviceListener.class);
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);

		Capture<SSDPPacket> capturePacket = new Capture<>(CaptureType.ALL);

		doRegister(plistener, dlistener);

		// notify bye
		String msg = getNotifyMessage(address, "ssdp:byebye");
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);
	}

	@Test
	public void testDeviceMessageWithBadStatus() throws Exception
	{
		testOpen();

		IPacketListener plistener = m_control.createMock("IPacketListener", IPacketListener.class);
		IDeviceListener<Object> dlistener = m_control.createMock("IDeviceListener", IDeviceListener.class);
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);

		Capture<SSDPPacket> capturePacket = new Capture<>(CaptureType.ALL);

		doRegister(plistener, dlistener);

		// notify bad
		String msg = getNotifyMessage(address, "ssdp:hola");
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);
	}

	@Test
	public void testDeviceMessageBadLocation() throws Exception
	{
		testOpen();

		IPacketListener plistener = m_control.createMock("IPacketListener", IPacketListener.class);
		IDeviceListener<Object> dlistener = m_control.createMock("IDeviceListener", IDeviceListener.class);
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);

		Capture<SSDPPacket> capturePacket = new Capture<>(CaptureType.ALL);

		doRegister(plistener, dlistener);

		String msg = getNotifyMessage(address, "ssdp:alive");
		msg = msg.replaceAll("file:.*", "file:DoesNotExist");
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, null, capturePacket);
		List<LoggingEvent> events = m_appender.getEvents();
		assertEquals(1, events.size());
		assertThat(events.get(0).getMessage().toString(), StringContains.containsString("DoesNotExist"));
		events.clear();
	}

	@Test
	public void testInstanceClose() throws Exception
	{
		testOpen();

		IPacketListener plistener = m_control.createMock("IPacketListener", IPacketListener.class);
		IDeviceListener<Object> dlistener = m_control.createMock("IDeviceListener", IDeviceListener.class);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);

		doRegister(plistener, dlistener);

		plistener.close();
		dlistener.close();
		m_control.replay();
		callback.close();
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testRepeatSearch() throws Exception
	{
		m_manager.setSearchFrequency(200, TimeUnit.MILLISECONDS);
		testOpen();
		Capture<InetSocketAddress> capture2ndUDP = new Capture<>(CaptureType.ALL);
		Capture<ByteBuffer> captureMCastBytes = new Capture<>(CaptureType.ALL);

		m_datagramInstance.send(EasyMock.capture(captureMCastBytes), EasyMock.capture(capture2ndUDP));
		EasyMock.expectLastCall().atLeastOnce();
		
		m_control.replay();
		waitCapture(captureMCastBytes, m_captureUDP.getValues().size());
		Thread.sleep(30);
		m_control.verify();
		m_control.reset();

		assertEquals(new HashSet<>(m_captureMCast.getValues()), new HashSet<>(capture2ndUDP.getValues()));
	}

	@Test
	public void testDeviceTimeout() throws Exception
	{
		testOpen();

		IPacketListener plistener = m_control.createMock("IPacketListener", IPacketListener.class);
		IDeviceListener<Object> dlistener = m_control.createMock("IDeviceListener", IDeviceListener.class);
		InetSocketAddress address = m_captureMCast.getValues().get(0);
		ISocketChannelCallback callback = m_captureCallback.getValues().get(0);

		String msg = getResponseMessage();
		msg = msg.replace("max-age=2000", "max-age=1");

		Object o = new Object();
		Capture<Device> captureDevice = new Capture<>(CaptureType.ALL);
		Capture<SSDPPacket> capturePacket = new Capture<>(CaptureType.ALL);

		doRegister(plistener, dlistener);

		EasyMock.expect(dlistener.deviceAdded(EasyMock.capture(captureDevice), EasyMock.eq(address))).andReturn(o);
		plistener.handlePacket(EasyMock.capture(capturePacket), EasyMock.eq(address));
		doDevicePacket(callback, msg, address, captureDevice, capturePacket);

		dlistener.deviceRemoved(o);
		m_control.replay();
		Thread.sleep(1500);
		m_control.verify();
		m_control.reset();
	}

	private String getResponseMessage()
	{
		URL deviceDesc = getClass().getResource("device.xml");
		String msg = "HTTP/1.1 200 OK\r\n"
				+ "DATE: Sat, 15 Dec 2007 14:35:15 GMT\r\n"
				+ "CACHE-CONTROL: max-age=2000\r\n" 
				+ "LOCATION: " + deviceDesc + "\r\n" 
				+ "EXT: \r\n" 
				+ "ST: upnp:rootdevice\r\n"
				+ "USN:uuid:5f9ec1b3-ed59-79bc-4530-745e1c1c832c\r\n" 
				+ "\r\n";
		return msg;
	}
	
	private String getNotifyMessage(InetSocketAddress address, String nts)
	{
		URL deviceDesc = getClass().getResource("device.xml");
		String msg = "NOTIFY * HTTP/1.1\r\n"
				+"Host:"+address.getAddress()+":"+address.getPort()+"\r\n"
				+"NT:upnp:rootdevice\r\n"
				+"NTS:"+nts+"\r\n"
				+"Location: "+deviceDesc+"\r\n"
				+"USN:uuid:5f9ec1b3-ed59-79bc-4530-745e1c1c832c\r\n"
				+ "Cache-Control:max-age=900\r\n\r\n";
		return msg;
	}
	
	private void expectClose(DatagramListenerChannelInstance instance, Capture<?> captures)
	{
		int count = captures.getValues().size();
		if (count > 0)
		{
			instance.close();
			EasyMock.expectLastCall().times(count);
		}
	}

	/**
	 * Do a socket message about a device
	 * 
	 * @param callback
	 * @param msg
	 * @param address
	 * @param captureDevice
	 * @param capturePacket
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void doDevicePacket(ISocketChannelCallback callback, String msg, InetSocketAddress address,
			Capture<?> captureDevice, Capture<?> capturePacket) throws IOException, InterruptedException
	{
		int next = capturePacket.getValues().size() + 1;
		m_control.replay();
		callback.handleRead(msg.getBytes(), address);
		waitCapture(capturePacket, next);
		if (captureDevice != null)
			waitCapture(captureDevice);
		else
			Thread.sleep(50);// wait for a delay for any threads to get done.
		m_control.verify();
		m_control.reset();
	}

	private void doRegister(IPacketListener plistener, IDeviceListener<?> dlistener)
	{
		m_control.replay();
		m_manager.addPacketListener(plistener);
		if (dlistener != null)
			m_manager.addDeviceListener(dlistener);
		m_control.verify();
		m_control.reset();

	}

}
