/**
 * 
 */
package svenz.remote.device.ipremote;

import static org.hamcrest.core.CombinableMatcher.both;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ScheduledExecutorService;
import org.easymock.EasyMock;
import org.easymock.IExpectationSetters;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Sven Zethelius
 * 
 */
public class QueuingWritableByteChannelUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final WritableByteChannel m_mockChannel = m_control.createMock("Channel", WritableByteChannel.class);
	private final QueuingWritableByteChannel m_channel = new QueuingWritableByteChannel();
	private final ScheduledExecutorService m_executor = m_control
			.createMock("Executor", ScheduledExecutorService.class);

	@Before
	public void setup()
	{
		m_channel.setChannel(m_mockChannel);
		m_channel.setExecutor(m_executor);
	}
	@Test
	public void testWriteSend() throws Exception
	{
		expectWrite("A").andReturn(1);
		write("A");
		verifyOutstanding("A");
		acknowledgeResponse(true);
		verifyOutstanding(null);
	}

	@Test
	public void testWriteWriteSendSend() throws Exception
	{
		expectWrite("A").andReturn(1);
		write("A");
		verifyOutstanding("A");
		write("B");
		verifyOutstanding("A");

		expectWrite("B").andReturn(1);
		acknowledgeResponse(true);
		verifyOutstanding("B");
		acknowledgeResponse(true);
		verifyOutstanding(null);
	}

	@Test
	public void testWriteException() throws Exception
	{
		IOException e = new IOException("Test Exception");
		expectWrite("A").andThrow(e);

		try
		{
			write("A");
			fail("IOException expected");
		}
		catch (IOException ex)
		{
			assertSame(e, ex);
		}
		expectWrite("A").andReturn(1);
		sendNext();
	}

	@Test
	public void testOpen() throws Exception
	{
		EasyMock.expect(m_mockChannel.isOpen()).andReturn(true);
		m_control.replay();
		assertEquals(true, m_channel.isOpen());
		m_control.verify();
		m_control.reset();

		EasyMock.expect(m_mockChannel.isOpen()).andReturn(false);
		m_control.replay();
		assertEquals(false, m_channel.isOpen());
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testClose() throws Exception
	{
		m_mockChannel.close();
		m_control.replay();
		m_channel.close();
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testToString() throws Exception
	{
		expectWrite("AB").andReturn(2);
		
		write("AB");
		write("CD");
		write("EF");

		m_control.replay();
		assertThat(m_channel.toString(),
				both(containsString("Channel")).and(containsString("CD")).and(containsString("EF")));
		m_control.verify();
		m_control.reset();
	}

	// TODO tests for retry

	private IExpectationSetters<Integer> expectWrite(String s) throws IOException
	{
		m_mockChannel.write(ByteBuffer.wrap(s.getBytes()));
		return EasyMock.expectLastCall();
	}

	private void write(String s) throws IOException
	{
		m_control.replay();
		try
		{
			m_channel.write(ByteBuffer.wrap(s.getBytes()));
		}
		finally
		{
			m_control.verify();
			m_control.reset();
		}
	}

	private void sendNext() throws IOException
	{
		m_control.replay();
		try
		{
			m_channel.sendNext();
		}
		finally
		{
			m_control.verify();
			m_control.reset();
		}
	}

	private void verifyOutstanding(String expected)
	{
		byte[] b = m_channel.getOutstandingRequest();
		assertEquals(expected, b != null ? new String(b) : null);
	}

	private void acknowledgeResponse(boolean success) throws IOException
	{
		m_control.replay();
		try
		{
			m_channel.acknowledgeResponse(success);
		}
		finally
		{
			m_control.verify();
			m_control.reset();
		}
	}

}
