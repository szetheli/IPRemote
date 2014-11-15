/**
 * 
 */
package svenz.remote.net.nio;

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;
import svenz.remote.net.nio.CompoundTCPSocketChannelCallback;
import svenz.remote.net.nio.ITCPSocketChannelCallback;
import svenz.remote.net.nio.TCPSocketChannelInstance;

/**
 * @author Sven Zethelius
 *
 */
@SuppressWarnings("resource")
public class CompoundTCPSocketChannelCallbackUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final ITCPSocketChannelCallback m_mockCallback1 =
			m_control.createMock("Callback1", ITCPSocketChannelCallback.class);
	private final ITCPSocketChannelCallback m_mockCallback2 =
			m_control.createMock("Callback2", ITCPSocketChannelCallback.class);
	private final InetSocketAddress m_address = m_control.createMock("Address", InetSocketAddress.class);
	private final TCPSocketChannelInstance instance = m_control.createMock("Instance", TCPSocketChannelInstance.class);


	@Test
	public void testHandleRead() throws Exception
	{
		byte[] b = "Test".getBytes();
		
		m_mockCallback1.handleRead(EasyMock.aryEq(b), EasyMock.same(m_address));
		m_mockCallback2.handleRead(EasyMock.aryEq(b), EasyMock.same(m_address));
		m_control.replay();
		new CompoundTCPSocketChannelCallback(
				Arrays.asList(m_mockCallback1, m_mockCallback2))
				.handleRead(b, m_address);
		m_control.verify();
		m_control.reset();
	}
	
	@Test
	public void testClose() throws Exception
	{
		m_mockCallback1.close();
		m_mockCallback2.close();
		m_control.replay();
		new CompoundTCPSocketChannelCallback(
				Arrays.asList(m_mockCallback1, m_mockCallback2))
				.close();
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testConnectionOpen() throws Exception
	{
		m_mockCallback1.connectionOpen(instance);
		m_mockCallback2.connectionOpen(instance);
		m_control.replay();
		new CompoundTCPSocketChannelCallback(Arrays.asList(m_mockCallback1, m_mockCallback2)).connectionOpen(instance);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testConnectionClose() throws Exception
	{
		m_mockCallback1.connectionClose(instance);
		m_mockCallback2.connectionClose(instance);
		m_control.replay();
		new CompoundTCPSocketChannelCallback(Arrays.asList(m_mockCallback1, m_mockCallback2)).connectionClose(instance);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testConnectionFailed() throws Exception
	{
		m_mockCallback1.connectionFailed(instance);
		m_mockCallback2.connectionFailed(instance);
		m_control.replay();
		new CompoundTCPSocketChannelCallback(Arrays.asList(m_mockCallback1, m_mockCallback2))
				.connectionFailed(instance);
		m_control.verify();
		m_control.reset();
	}
}
