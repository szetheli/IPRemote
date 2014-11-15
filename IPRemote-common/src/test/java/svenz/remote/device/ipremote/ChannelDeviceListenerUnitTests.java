/**
 * 
 */
package svenz.remote.device.ipremote;

import static org.junit.Assert.assertEquals;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import javax.xml.namespace.QName;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import svenz.remote.device.IChangable.IChangeListener;
import svenz.remote.net.nio.ITCPSocketChannelCallback;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.nio.TCPSocketChannelInstance;
import svenz.remote.net.protocol.ssdp.SSDPManager;
import svenz.remote.net.protocol.ssdp.jaxb.Device;


/**
 * @author Sven Zethelius
 *
 */
public class ChannelDeviceListenerUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final SSDPManager m_ssdpManager = m_control.createMock("SSDPManager", SSDPManager.class);
	private final SocketChannelManager m_channelManager =
			m_control.createMock("ChannelManager", SocketChannelManager.class);
	private final ITCPSocketChannelCallback m_callback = 
			m_control.createMock("Callback", ITCPSocketChannelCallback.class);
	private final Device m_device = m_control.createMock("Device", Device.class);
	private final TCPSocketChannelInstance m_instance = 
			m_control.createMock("Instance", TCPSocketChannelInstance.class);
	
	private final Capture<ITCPSocketChannelCallback> m_captureCallback = 
			new Capture<ITCPSocketChannelCallback>(CaptureType.ALL);
	
	private final ChannelDeviceListener m_listener = new ChannelDeviceListener();

	@Before
	public void setup()
	{
		m_control.replay();
		m_listener.setChannelCallback(m_callback);
		m_listener.setChannelManager(m_channelManager);
		m_listener.setSSDPManager(m_ssdpManager);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testFixedPort() throws Exception
	{
		InetAddress localHost = InetAddress.getLocalHost();
		EasyMock.expect(m_device.getUDN()).andReturn("MockDevice");
		EasyMock.expect(m_device.getFriendlyName()).andReturn("Mock Device").anyTimes();
		EasyMock.expect(m_channelManager.connect(
				EasyMock.eq(new InetSocketAddress(localHost, 6789)), 
				EasyMock.capture(m_captureCallback))).andReturn(m_instance);

		m_instance.close();
		m_control.replay();
		m_listener.setPort(6789);
		Object handle = m_listener.deviceAdded(m_device, new InetSocketAddress(localHost, 445));
		
		assertEquals(m_instance, m_listener.getInstance());

		m_listener.deviceRemoved(handle);
		assertEquals(null, m_listener.getInstance());
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testElementReader() throws Exception
	{
		InetAddress localHost = InetAddress.getLocalHost();

		EasyMock.expect(m_device.getUDN()).andReturn("MockDevice");
		EasyMock.expect(m_device.getFriendlyName()).andReturn("Mock Device").anyTimes();
		EasyMock.expect(m_device.getIPPort()).andReturn(6789);

		EasyMock.expect(
				m_channelManager.connect(EasyMock.eq(new InetSocketAddress(localHost, 6789)),
						EasyMock.capture(m_captureCallback))).andReturn(m_instance);

		QName qname = new QName("abc", "def");

		m_instance.close();
		m_control.replay();
		m_listener.setElementName(qname);
		Object handle = m_listener.deviceAdded(m_device, new InetSocketAddress(localHost, 445));

		assertEquals(m_instance, m_listener.getInstance());

		m_listener.deviceRemoved(handle);
		assertEquals(null, m_listener.getInstance());
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testElementReaderNotFound() throws Exception
	{
		InetAddress localHost = InetAddress.getLocalHost();

		EasyMock.expect(m_device.getFriendlyName()).andReturn("Mock Device").anyTimes();
		EasyMock.expect(m_device.getIPPort()).andReturn(null);

		QName qname = new QName("abc", "def");

		m_control.replay();
		m_listener.setElementName(qname);
		assertEquals(null, m_listener.deviceAdded(m_device, new InetSocketAddress(localHost, 445)));

		assertEquals(null, m_listener.getInstance());

		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testChangeListener() throws Exception
	{
		InetAddress localHost = InetAddress.getLocalHost();

		IChangeListener changeListener1 = m_control.createMock("ChangeListener1", IChangeListener.class);
		IChangeListener changeListener2 = m_control.createMock("ChangeListener1", IChangeListener.class);

		EasyMock.expect(m_device.getUDN()).andReturn("MockDevice").anyTimes();
		EasyMock.expect(m_device.getFriendlyName()).andReturn("Mock Device").anyTimes();
		EasyMock.expect(
				m_channelManager.connect(EasyMock.eq(new InetSocketAddress(localHost, 6789)),
						EasyMock.capture(m_captureCallback))).andReturn(m_instance).anyTimes();

		changeListener1.stateChanged(m_listener, ChannelDeviceListener.NOTIFY_ADDRESS);
		EasyMock.expectLastCall().times(4);
		changeListener1.stateChanged(m_listener, ChannelDeviceListener.NOTIFY_INSTANCE);
		EasyMock.expectLastCall().times(4);
		changeListener2.stateChanged(m_listener, ChannelDeviceListener.NOTIFY_ADDRESS);
		EasyMock.expectLastCall().times(2);
		changeListener2.stateChanged(m_listener, ChannelDeviceListener.NOTIFY_INSTANCE);
		EasyMock.expectLastCall().times(2);
		m_instance.close();
		m_instance.close();

		m_control.replay();
		m_listener.setPort(6789);
		m_listener.addChangeListener(changeListener1);
		m_listener.addChangeListener(changeListener2);

		Object handle = m_listener.deviceAdded(m_device, new InetSocketAddress(localHost, 445));

		assertEquals(m_instance, m_listener.getInstance());

		m_listener.deviceRemoved(handle);
		assertEquals(null, m_listener.getInstance());

		m_listener.removeChangeListener(changeListener2);
		handle = m_listener.deviceAdded(m_device, new InetSocketAddress(localHost, 445));
		assertEquals(m_instance, m_listener.getInstance());
		m_listener.deviceRemoved(handle);

		m_control.verify();
		m_control.reset();

	}
}
