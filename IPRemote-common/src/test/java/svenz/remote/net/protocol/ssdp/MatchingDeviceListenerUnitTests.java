/**
 * 
 */
package svenz.remote.net.protocol.ssdp;

import static org.junit.Assert.assertEquals;
import java.net.InetSocketAddress;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import svenz.remote.net.protocol.ssdp.MatchingDeviceListener;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;
import svenz.remote.net.protocol.ssdp.jaxb.Device;

/**
 * @author Sven Zethelius
 *
 */
@SuppressWarnings("unchecked")
public class MatchingDeviceListenerUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();

	private final IDeviceListener<Object> m_mockListener = m_control.createMock("Listener", IDeviceListener.class);
	private final Device m_device = m_control.createMock("Device", Device.class);
	private final InetSocketAddress m_address = m_control.createMock("Address", InetSocketAddress.class);
	private final MatchingDeviceListener<Object> m_listener = new MatchingDeviceListener<Object>(m_mockListener);

	@Before
	public void setup()
	{
		m_listener.setDeviceType("DeviceType");
		m_listener.setFriendlyName("FriendlyName");
		m_listener.setModelName("ModelName");
	}

	@Test
	public void testMatches() throws Exception
	{
		Object o = new Object();
		EasyMock.expect(m_device.getDeviceType()).andReturn("DeviceType");
		EasyMock.expect(m_device.getFriendlyName()).andReturn("FriendlyName");
		EasyMock.expect(m_device.getModelName()).andReturn("ModelName");

		EasyMock.expect(m_mockListener.deviceAdded(EasyMock.same(m_device), EasyMock.same(m_address))).andReturn(o);
		m_control.replay();
		assertEquals(o, m_listener.deviceAdded(m_device, m_address));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testNoMatch() throws Exception
	{
		EasyMock.expect(m_device.getDeviceType()).andReturn("DeviceType1").anyTimes();
		EasyMock.expect(m_device.getFriendlyName()).andReturn("FriendlyName").anyTimes();
		EasyMock.expect(m_device.getModelName()).andReturn("ModelName").anyTimes();

		m_control.replay();
		assertEquals(null, m_listener.deviceAdded(m_device, m_address));
		m_control.verify();
		m_control.reset();

		EasyMock.expect(m_device.getDeviceType()).andReturn("DeviceType").anyTimes();
		EasyMock.expect(m_device.getFriendlyName()).andReturn("FriendlyName1").anyTimes();
		EasyMock.expect(m_device.getModelName()).andReturn("ModelName").anyTimes();

		m_control.replay();
		assertEquals(null, m_listener.deviceAdded(m_device, m_address));
		m_control.verify();
		m_control.reset();

		EasyMock.expect(m_device.getDeviceType()).andReturn("DeviceType").anyTimes();
		EasyMock.expect(m_device.getFriendlyName()).andReturn("FriendlyName").anyTimes();
		EasyMock.expect(m_device.getModelName()).andReturn("ModelName1").anyTimes();

		m_control.replay();
		assertEquals(null, m_listener.deviceAdded(m_device, m_address));
		m_control.verify();
		m_control.reset();

		EasyMock.expect(m_device.getDeviceType()).andReturn(null).anyTimes();
		EasyMock.expect(m_device.getFriendlyName()).andReturn(null).anyTimes();
		EasyMock.expect(m_device.getModelName()).andReturn(null).anyTimes();

		m_control.replay();
		assertEquals(null, m_listener.deviceAdded(m_device, m_address));
		m_control.verify();
		m_control.reset();
	}
}
