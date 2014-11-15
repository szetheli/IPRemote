/**
 * 
 */
package svenz.remote.device.ipremote;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import svenz.remote.net.nio.ITCPSocketChannelCallback;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.nio.TCPSocketChannelInstance;
import svenz.remote.net.protocol.ssdp.SSDPManager;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;
import svenz.remote.net.protocol.ssdp.jaxb.Device;

/**
 * @author Sven Zethelius
 *
 */
public class AbstractDeviceUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final IDeviceListener<?> m_deviceListener = m_control.createMock("DeviceListener", IDeviceListener.class);
	private final SSDPManager m_ssdpManager = m_control.createMock("SSDPManager", SSDPManager.class);
	private final SocketChannelManager m_channelManager = 
			m_control.createMock("ChannelManager", SocketChannelManager.class);
	private final TCPSocketChannelInstance m_instance = m_control.createMock("Instance", TCPSocketChannelInstance.class);
	private final Device m_deviceDesc = m_control.createMock("Device", Device.class);
	private final Capture<ITCPSocketChannelCallback> captureCallback = 
			new Capture<ITCPSocketChannelCallback>(CaptureType.ALL);

	private final Capture<String> m_captureResponse = new Capture<String>(CaptureType.ALL);
	private final Capture<ChannelDeviceListener> m_captureChannelListener = 
			new Capture<ChannelDeviceListener>(CaptureType.ALL);

	private InetSocketAddress m_address;


	private final DeviceImpl m_device = new DeviceImpl();

	@Before
	public void setup() throws Exception
	{
		m_address = new InetSocketAddress(InetAddress.getLocalHost(), 2345);
		m_ssdpManager.addDeviceListener(m_deviceListener);
		m_control.replay();
		m_device.setChannelManager(m_channelManager);
		m_device.setSSDPManager(m_ssdpManager);
		m_device.open();
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testDevice() throws Exception
	{
		expectDeviceAdd();
		
		EasyMock.expect(m_instance.isOpen()).andReturn(true);
		m_instance.close();

		m_control.replay();
		ChannelDeviceListener channelListener = m_captureChannelListener.getValue();
		Object handle = channelListener.deviceAdded(m_deviceDesc, m_address);

		assertEquals(m_instance, m_device.getWriteChannel());
		assertEquals(true, m_device.isConnected());

		captureCallback.getValue().handleRead("ABC".getBytes(), m_address);
		assertEquals(true, m_captureResponse.getValues().remove("ABC"));

		captureCallback.getValue().handleRead("ABC\r\n".getBytes(), m_address);
		assertEquals(true, m_captureResponse.getValues().remove("ABC"));

		captureCallback.getValue().handleRead("".getBytes(), m_address);
		assertEquals(true, m_captureResponse.getValues().isEmpty());

		captureCallback.getValue().handleRead("ABC\r\nDEF".getBytes(), m_address);
		assertEquals(true, m_captureResponse.getValues().remove("ABC"));
		assertEquals(true, m_captureResponse.getValues().remove("DEF"));

		captureCallback.getValue().handleRead("ABC\nDEF".getBytes(), m_address);
		assertEquals(true, m_captureResponse.getValues().remove("ABC"));
		assertEquals(true, m_captureResponse.getValues().remove("DEF"));

		captureCallback.getValue().handleRead("ABC\rDEF".getBytes(), m_address);
		assertEquals(true, m_captureResponse.getValues().remove("ABC"));
		assertEquals(true, m_captureResponse.getValues().remove("DEF"));

		channelListener.deviceRemoved(handle);
		assertEquals(false, m_device.isConnected());

		m_control.verify();
		m_control.reset();
	}



	@Test
	public void testRegister() throws Exception
	{
		AbstractCoded<?> coded1 = m_control.createMock("Coded1", AbstractCoded.class),
				coded2 = m_control.createMock("Coded2", AbstractCoded.class);
		expectDeviceAdd();

		coded1.setWriter(m_instance);
		coded2.setWriter(m_instance);

		coded1.setWriter(null);
		coded2.setWriter(null);

		m_instance.close();

		m_control.replay();
		m_device.register(coded1, coded2);
		
		ChannelDeviceListener channelListener = m_captureChannelListener.getValue();
		Object handle = channelListener.deviceAdded(m_deviceDesc, m_address);
		
		channelListener.deviceRemoved(handle);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testClose() throws Exception
	{

		AbstractCoded<?> coded1 = m_control.createMock("Coded1", AbstractCoded.class), coded2 =
				m_control.createMock("Coded2", AbstractCoded.class);
		expectDeviceAdd();

		m_ssdpManager.removeDeviceListener(m_deviceListener);
		coded1.setWriter(m_instance);
		coded2.setWriter(m_instance);

		coded1.setWriter(null);
		coded2.setWriter(null);

		m_instance.close();

		m_control.replay();
		m_device.register(coded1, coded2);

		ChannelDeviceListener channelListener = m_captureChannelListener.getValue();
		Object handle = channelListener.deviceAdded(m_deviceDesc, m_address);

		m_device.close();
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalStateException.class)
	public void testGetConfigurationFileNotFound() throws Exception
	{
		m_control.replay();
		m_device.getConfiguration("FileNotFound");
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testGetConfiguration() throws Exception
	{
		m_control.replay();
		Configuration config = m_device.getConfiguration("AbstractDeviceUnitTests.properties");
		m_control.verify();
		m_control.reset();

		MapConfiguration mconfig = new MapConfiguration(new HashMap<String, Object>());
		mconfig.copy(config);
		assertEquals(new HashSet<String>(Arrays.asList("a", "c")), mconfig.getMap().keySet());
	}

	private void expectDeviceAdd() throws IOException
	{
		EasyMock.expect(m_deviceDesc.getUDN()).andReturn("MockDevice").anyTimes();
		EasyMock.expect(m_deviceDesc.getFriendlyName()).andReturn("Mock Device").anyTimes();

		EasyMock.expect(
				m_channelManager.connect(
						EasyMock.eq(new InetSocketAddress(m_address.getAddress(), 8005)), 
							EasyMock.capture(captureCallback)))
					.andReturn(m_instance);
	}
	
	private class DeviceImpl extends AbstractDevice
	{
		@Override
		protected IDeviceListener<?> initDeviceListener(ChannelDeviceListener listener)
		{
			listener.setPort(8005);
			m_captureChannelListener.setValue(listener);
			return m_deviceListener;
		}

		@Override
		protected void handleResponse(String response)
		{
			m_captureResponse.setValue(response);
		}
	}
}
