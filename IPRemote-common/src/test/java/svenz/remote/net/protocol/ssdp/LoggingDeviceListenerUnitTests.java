/**
 * 
 */
package svenz.remote.net.protocol.ssdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import java.net.InetSocketAddress;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.hamcrest.core.CombinableMatcher;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import svenz.remote.net.protocol.ssdp.LoggingDeviceListener;
import svenz.remote.net.protocol.ssdp.jaxb.Device;
import svenz.test.helper.CaptureAppender;

/**
 * @author Sven Zethelius
 *
 */
public class LoggingDeviceListenerUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final LoggingDeviceListener m_listener = new LoggingDeviceListener();
	private final CaptureAppender m_appender = CaptureAppender.install(LoggingDeviceListener.class.getName());
	private final Device m_device = m_control.createMock("Device", Device.class);
	private final InetSocketAddress m_address = m_control.createMock("Address", InetSocketAddress.class);


	@Before
	public void setup()
	{
		m_appender.setThreshold(Level.INFO);
	}

	@After
	public void teardown()
	{
		CaptureAppender.reset();
	}

	@Test
	public void testLogging() throws Exception
	{
		EasyMock.expect(m_device.getDeviceType()).andReturn("DeviceType");
		EasyMock.expect(m_device.getFriendlyName()).andReturn("DeviceName");
		EasyMock.expect(m_device.getModelName()).andReturn("DeviceModel");

		m_control.replay();
		assertEquals(m_device, m_listener.deviceAdded(m_device, m_address));
		m_control.verify();
		m_control.reset();

		List<LoggingEvent> events = m_appender.getEvents();
		assertThat(
				events.get(0).getMessage().toString(),
				CombinableMatcher.both(StringContains.containsString("DeviceType"))
						.and(StringContains.containsString("DeviceName"))
						.and(StringContains.containsString("DeviceModel")));
		events.clear();

		EasyMock.expect(m_device.getDeviceType()).andReturn("DeviceType");
		EasyMock.expect(m_device.getFriendlyName()).andReturn("DeviceName");
		EasyMock.expect(m_device.getModelName()).andReturn("DeviceModel");
		m_control.replay();
		m_listener.deviceRemoved(m_device);
		m_control.verify();
		m_control.reset();
		assertThat(
				events.get(0).getMessage().toString(),
				CombinableMatcher.both(StringContains.containsString("DeviceType"))
						.and(StringContains.containsString("DeviceName"))
						.and(StringContains.containsString("DeviceModel")));

	}
}
