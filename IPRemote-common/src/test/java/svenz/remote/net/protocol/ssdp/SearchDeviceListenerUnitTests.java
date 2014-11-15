/**
 * 
 */
package svenz.remote.net.protocol.ssdp;

import static org.junit.Assert.assertEquals;
import java.net.InetSocketAddress;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;
import svenz.remote.net.protocol.ssdp.SSDPManager.SSDPSearchRequest;
import svenz.remote.net.protocol.ssdp.jaxb.Device;

/**
 * @author Sven Zethelius
 *
 */
public class SearchDeviceListenerUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final SSDPManager m_ssdpManager = m_control.createMock("SSDPManager", SSDPManager.class);
	private final Device m_device = m_control.createMock("Device", Device.class);
	private final InetSocketAddress m_address = m_control.createMock("Address", InetSocketAddress.class);
	private final Capture<SSDPSearchRequest> m_capturePacket = new Capture<>(CaptureType.ALL);

	private final SearchDeviceListener m_listener = new SearchDeviceListener(m_ssdpManager);

	@Test
	public void testRemoved() throws Exception
	{
		EasyMock.expect(m_device.getUDN()).andReturn("UDN");
		m_ssdpManager.search(EasyMock.capture(m_capturePacket));
		m_control.replay();
		String handle = m_listener.deviceAdded(m_device, m_address);
		m_listener.deviceRemoved(handle);
		m_control.verify();
		m_control.reset();
		assertEquals("UDN", m_capturePacket.getValue().getDevice());
		m_capturePacket.reset();

		EasyMock.expect(m_device.getUDN()).andReturn("UDN");
		m_control.replay();
		handle = m_listener.deviceAdded(m_device, m_address);
		m_listener.deviceRemoved(handle);
		m_control.verify();
		m_control.reset();

		EasyMock.expect(m_device.getUDN()).andReturn("UDN2");
		m_ssdpManager.search(EasyMock.capture(m_capturePacket));
		m_control.replay();
		handle = m_listener.deviceAdded(m_device, m_address);
		m_listener.deviceRemoved(handle);
		m_control.verify();
		m_control.reset();
		assertEquals("UDN2", m_capturePacket.getValue().getDevice());
	}
}
