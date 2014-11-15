/**
 * 
 */
package svenz.remote.device.impl;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.apache.log4j.Level;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import svenz.remote.common.utilities.BeanAdapter;
import svenz.remote.device.IMenu;
import svenz.remote.device.IPlayable;
import svenz.remote.device.ipremote.AbstractDevice;
import svenz.remote.net.nio.ITCPSocketChannelCallback;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.protocol.ssdp.SSDPManager;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;
import svenz.remote.net.protocol.ssdp.jaxb.Device;
import svenz.test.helper.CaptureAppender;

/**
 * @author Sven Zethelius
 *
 */
public abstract class AbstractDeviceUnitTests<T extends AbstractDevice>
{
	protected final IMocksControl m_control = EasyMock.createControl();
	protected final T m_test = create();
	protected final ScheduledThreadPoolExecutor m_executor = new ScheduledThreadPoolExecutor(2);
	protected final SocketChannelManager m_channelManager = m_control.createMock("ChannelManager",
			SocketChannelManager.class);
	protected final SSDPManager m_ssdpManager = m_control.createMock("SSDPManager", SSDPManager.class);
	protected final Device m_device = m_control.createMock("Device", Device.class);
	protected final WritableByteChannel m_channel = m_control.createMock("Channel", WritableByteChannel.class);
	protected final Capture<IDeviceListener<?>> m_listener = new Capture<SSDPManager.IDeviceListener<?>>(CaptureType.ALL);
	protected final Capture<ITCPSocketChannelCallback> m_callback = 
			new Capture<ITCPSocketChannelCallback>(CaptureType.ALL);
	protected final Capture<ByteBuffer> m_writes = new Capture<ByteBuffer>(CaptureType.ALL);
	protected InetSocketAddress m_address;
	protected CaptureAppender m_appender;

	protected abstract T create();

	@Before
	public void setUp() throws Exception
	{
		m_appender = CaptureAppender.install("svenz", m_test.getClass().getName());
		m_appender.setThreshold(Level.WARN);

		m_ssdpManager.addDeviceListener(EasyMock.capture(m_listener));
		m_control.replay();
		m_test.setExecutor(m_executor);
		m_test.setChannelManager(m_channelManager);
		m_test.setSSDPManager(m_ssdpManager);
		init();
		m_test.open();
		m_control.verify();
		m_control.reset();
	}

	protected void init()
	{

	}

	@After
	public void teardown()
	{
		assertEquals(Collections.EMPTY_LIST, m_appender.getEvents());
		CaptureAppender.reset();
	}

	protected void waitForCapture(Capture<?> capture, int count) throws InterruptedException
	{
		for (int i = 0; i < 200 && capture.getValues().size() < count; i++)
			Thread.sleep(50);
		assertEquals(count, capture.getValues().size());
	}

	protected void expectWrite(String... writes) throws IOException
	{
		expectWrite(Arrays.asList(writes));
	}

	protected void expectWrite(Collection<String> writes) throws IOException
	{
		for (String write : writes)
		{
			EasyMock.expect(
					m_channel.write(EasyMock.and(EasyMock.eq(ByteBuffer.wrap(write.getBytes())),
							EasyMock.capture(m_writes)))).andReturn(write.length());
		}
	}

	protected void read(String... s) throws IOException
	{
		read(Arrays.asList(s));
	}

	protected void read(Collection<String> reads) throws IOException
	{
		for (String s : reads)
			m_callback.getValue().handleRead(s.getBytes(), m_address);
	}

	@Test
	public void testDeviceListenerNoMatch() throws Exception
	{
		EasyMock.expect(m_device.getDeviceType()).andReturn("NoMatch").anyTimes();
		EasyMock.expect(m_device.getFriendlyName()).andReturn("NoMatch").anyTimes();
		EasyMock.expect(m_device.getModelName()).andReturn("NoMatch").anyTimes();
		EasyMock.expect(m_device.getUDN()).andReturn("TestUDN").anyTimes();

		m_control.replay();
		m_listener.getValue().deviceAdded(m_device, new InetSocketAddress(InetAddress.getLocalHost(), 1234));
		m_control.verify();
		m_control.reset();
	}

	protected <O> void testCodedStatus(Object root, String path, O start, O end, List<String> writes,
			List<String> reads)
			throws Exception
	{
		BeanAdapter adapter = new BeanAdapter();
		adapter.addRoot("root", root);

		if (writes != null)
			expectWrite(writes);
		m_control.replay();
		assertEquals(start, adapter.resolve(Arrays.asList("root", path)));
		read(reads);
		assertEquals(end, adapter.resolve(Arrays.asList("root", path)));
		m_control.verify();
		m_control.reset();
	}

	protected void testMenu(IMenu menu, IMenu.Action action, List<String> writes, List<String> reads)
			throws IOException
	{
		expectWrite(writes);
		m_control.replay();
		menu.action(action);
		read(reads);
		m_control.verify();
		m_control.reset();
	}

	protected void testMenu(IMenu menu, IMenu.Action action, String write, String read) throws IOException
	{
		testMenu(menu, action, Arrays.asList(write), Arrays.asList(read));
	}

	protected void testPlayable(IPlayable playable, IPlayable.Action action, String code, String response,
			IPlayable.Action expected) throws IOException
	{
		expectWrite(code);
		m_control.replay();
		playable.setAction(action);
		read(response);
		assertEquals(expected, playable.getCurrentAction());
		m_control.verify();
		m_control.reset();
	}

}
