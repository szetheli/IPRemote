/**
 * 
 */
package svenz.remote.net.nio;

import static org.junit.Assert.assertEquals;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Test;
import svenz.remote.net.nio.AsyncSocketChannelCallback;
import svenz.remote.net.nio.ITCPSocketChannelCallback;
import svenz.remote.net.nio.TCPSocketChannelInstance;
import svenz.test.helper.CaptureAppender;

/**
 * @author Sven Zethelius
 *
 */
public class AsyncSocketChannelCallbackUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final Executor m_executor = m_control.createMock("Executor", Executor.class);
	private final ITCPSocketChannelCallback m_mockCallback = 
			m_control.createMock("Callback", ITCPSocketChannelCallback.class);
	private final InetSocketAddress m_address = m_control.createMock("Address", InetSocketAddress.class);
	private final TCPSocketChannelInstance m_instance = m_control.createMock("Channel", TCPSocketChannelInstance.class);

	private final Capture<Runnable> m_captureRunnable = new Capture<Runnable>(CaptureType.ALL);

	private final AsyncSocketChannelCallback m_callback = new AsyncSocketChannelCallback(m_executor, m_mockCallback);

	@After
	public void teardown()
	{
		CaptureAppender.reset();
	}

	@Test
	public void testRead() throws Exception
	{
		expectRun();

		m_control.replay();
		m_callback.handleRead("Test".getBytes(), m_address);
		m_control.verify();
		m_control.reset();

		m_mockCallback.handleRead(EasyMock.aryEq("Test".getBytes()), EasyMock.same(m_address));
		doAsync();
	}

	@Test
	public void testReadException() throws Exception
	{
		CaptureAppender appender = CaptureAppender.install(m_mockCallback.getClass().getName());
		expectRun();

		m_control.replay();
		m_callback.handleRead("Test".getBytes(), m_address);
		m_control.verify();
		m_control.reset();

		m_mockCallback.handleRead(EasyMock.aryEq("Test".getBytes()), EasyMock.same(m_address));
		EasyMock.expectLastCall().andThrow(new IllegalStateException("Test Exception"));

		doAsync();
		assertEquals(1, appender.getEvents().size());
	}

	@Test
	public void testConnectionOpen() throws Exception
	{
		expectRun();
		m_control.replay();
		m_callback.connectionOpen(m_instance);
		m_control.verify();
		m_control.reset();

		m_mockCallback.connectionOpen(m_instance);
		doAsync();
	}

	@Test
	public void testConnectionClose() throws Exception
	{
		expectRun();
		m_control.replay();
		m_callback.connectionClose(m_instance);
		m_control.verify();
		m_control.reset();

		m_mockCallback.connectionClose(m_instance);
		doAsync();
	}

	@Test
	public void testConnectionFailed() throws Exception
	{
		expectRun();
		m_control.replay();
		m_callback.connectionFailed(m_instance);
		m_control.verify();
		m_control.reset();

		m_mockCallback.connectionFailed(m_instance);
		doAsync();
	}

	@Test
	public void testClose() throws Exception
	{
		m_mockCallback.close();
		m_control.replay();
		m_callback.close();
		m_control.verify();
		m_control.reset();
	}

	private void expectRun()
	{
		m_executor.execute(EasyMock.capture(m_captureRunnable));
	}

	private void doAsync()
	{
		m_control.replay();
		try
		{
			m_captureRunnable.getValue().run();
		}
		finally
		{
			m_control.verify();
			m_control.reset();
		}
	}
}
