/**
 * 
 */
package svenz.remote.net.protocol.ssdp;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;
import svenz.remote.net.nio.TCPSocketChannelInstance;

/**
 * @author Sven Zethelius
 *
 */
public class SSDPChannelCallbackUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final SSDPManager m_ssdpManager = m_control.createMock("SSDPManager", SSDPManager.class);
	private final TCPSocketChannelInstance m_instance = 
			m_control.createMock("Instance", TCPSocketChannelInstance.class);
	private final SSDPChannelCallback m_callback = new SSDPChannelCallback(m_ssdpManager, "TestID");

	@Test
	public void testClose() throws Exception
	{
		m_ssdpManager.notifyDeviceRemoved("TestID");

		m_control.replay();
		m_callback.close();
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testConnectionClose() throws Exception
	{
		m_ssdpManager.notifyDeviceRemoved("TestID");

		m_control.replay();
		m_callback.connectionClose(m_instance);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testConnectionFailed() throws Exception
	{
		m_ssdpManager.notifyDeviceRemoved("TestID");

		m_control.replay();
		m_callback.connectionFailed(m_instance);
		m_control.verify();
		m_control.reset();
	}
}
