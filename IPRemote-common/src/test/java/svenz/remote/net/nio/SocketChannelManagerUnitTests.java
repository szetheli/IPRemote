package svenz.remote.net.nio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static svenz.test.helper.TestHelper.waitCapture;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Level;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.easymock.internal.Result;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.Utilities;
import svenz.test.helper.TestHelper;

public class SocketChannelManagerUnitTests
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SocketChannelManagerUnitTests.class);
	private final IMocksControl m_control = EasyMock.createControl();
	private final InetAddress m_address = InetAddress.getLoopbackAddress();
	private final SocketChannelManager m_manager = new SocketChannelManager();
	private final ScheduledThreadPoolExecutor m_executor = new ScheduledThreadPoolExecutor(2);

	@Rule
	public TestName m_name = new TestName();

	@Before
	public void setup() throws IOException
	{
		LOGGER.info("Starting {}", m_name.getMethodName());
		TestHelper.setLogger(getClass().getPackage().getName(), Level.TRACE);
		Utilities.configure(m_executor, 1, TimeUnit.SECONDS);
		m_manager.setExecutor(m_executor);
		m_manager.open();
	}

	@After
	public void teardown()
	{
		m_manager.close();
		m_executor.shutdown();
		TestHelper.resetLoggers();
		LOGGER.info("Ending {}", m_name.getMethodName());
	}

	@Test
	public void testOpenTwice() throws Exception
	{
		m_manager.open();
		testTCPInstanceClose();
	}

	/**
	 * Quickly open and close an instance (e.g. client code threw an exception
	 * and closed gracefully.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTCPInstanceClose() throws Exception
	{
		InetSocketAddress address = new InetSocketAddress(m_address, getFreePort());
		ITCPSocketChannelCallback callback = m_control.createMock("ITCPSocketChannelCallback", ITCPSocketChannelCallback.class);
		Capture<TCPSocketChannelInstance> capture = new Capture<TCPSocketChannelInstance>(CaptureType.ALL);

		callback.connectionOpen(EasyMock.capture(capture));
		callback.connectionClose(EasyMock.capture(capture));
		callback.close();

		WritableByteChannel connect = null;
		m_control.replay();
		try (ServerSocket s = new ServerSocket())
		{
			s.bind(address);
			try (WritableByteChannel c = m_manager.connect(address, 100, callback))
			{
				connect = c;
				try (Socket socket = s.accept())
				{
					waitCapture(capture);
				}
			}
		}
		m_control.verify();
		assertEquals(Arrays.asList(connect, connect), capture.getValues());
	}

	/**
	 * Test what happens when trying to connect to an IP that isn't listening
	 * 
	 * @throws Exception
	 */
	@Test(timeout = 200)
	public void testTCPConnectionCloseFromReject() throws Exception
	{
		InetSocketAddress address = new InetSocketAddress(m_address, getFreePort());
		ITCPSocketChannelCallback callback = m_control.createMock("ITCPSocketChannelCallback", ITCPSocketChannelCallback.class);
		Capture<TCPSocketChannelInstance> capture = new Capture<>(CaptureType.ALL);

		ThreadCaptureAnswer<Void> tanswer = new ThreadCaptureAnswer<>();
		callback.connectionFailed(EasyMock.capture(capture));
		EasyMock.expectLastCall().andAnswer(tanswer);
		callback.close();

		WritableByteChannel connect = null;
		m_control.replay();
		try (WritableByteChannel c = m_manager.connect(address, 100, callback))
		{
			connect = c;
			waitCapture(capture);
			// Wait a tick to let the close get through the callback in the
			// other thread
			Thread.sleep(30);
		}
		m_control.verify();
		assertEquals(Arrays.asList(connect), capture.getValues());
		assertNotEquals("Expected close on selector thread to be first", Thread.currentThread(), tanswer.getThreads().getValue());
	}

	@Test
	public void testTCPConnectionCloseFromManager() throws Exception
	{
		InetSocketAddress address = new InetSocketAddress(m_address, getFreePort());
		ITCPSocketChannelCallback callback = m_control.createMock("ITCPSocketChannelCallback", ITCPSocketChannelCallback.class);
		Capture<TCPSocketChannelInstance> capture = new Capture<>(CaptureType.ALL);

		callback.connectionOpen(EasyMock.capture(capture));
		callback.connectionClose(EasyMock.capture(capture));
		callback.close();

		WritableByteChannel connect = null;
		m_control.replay();
		try (ServerSocket s = new ServerSocket())
		{
			s.bind(address);
			WritableByteChannel c = m_manager.connect(address, 100, callback);
			connect = c;
			try (Socket socket = s.accept())
			{
				waitCapture(capture);
				m_manager.close();
			}
		}
		m_control.verify();
		assertEquals(Arrays.asList(connect, connect), capture.getValues());
	}

	@Test
	public void testListenDatagramAlreadyInUse() throws Exception
	{
		ISocketChannelCallback callback = m_control.createMock("Callback", ISocketChannelCallback.class);
		int port = getFreePort();
		InetSocketAddress address = new InetSocketAddress(m_address, port);

		m_control.replay();
		try (DatagramSocket ds = new DatagramSocket(address))
		{
			ds.setReuseAddress(false);
			try
			{
				m_manager.listenDatagram(address, callback);
				fail("BindException expected");
			}
			catch (BindException e)
			{
				LOGGER.info("Expected exception");
			}
		}
		finally
		{
			m_control.verify();
		}
	}

	@Test
	public void testListenDatagram() throws Exception
	{
		ISocketChannelCallback callback = m_control.createMock("Callback", ISocketChannelCallback.class);
		int port = getFreePort();
		InetSocketAddress address = new InetSocketAddress(m_address, port);

		Capture<byte[]> captureBytes = new Capture<>(CaptureType.ALL);
		Capture<InetSocketAddress> captureAddress = new Capture<>(CaptureType.ALL);
		callback.handleRead(EasyMock.capture(captureBytes), EasyMock.capture(captureAddress));
		callback.close();
		m_control.replay();
		try (DatagramListenerChannelInstance instance = m_manager.listenDatagram(address, callback))
		{
			try (DatagramSocket ds = new DatagramSocket())
			{
				byte[] bytes = "Test".getBytes();
				DatagramPacket p = new DatagramPacket(bytes, bytes.length);
				p.setSocketAddress(address);
				ds.send(p);
				waitCapture(captureAddress);
			}
		}
		m_control.verify();
		assertTrue(Arrays.equals("Test".getBytes(), captureBytes.getValue()));
	}

	@Test
	public void testListenMultigram() throws Exception
	{
		ISocketChannelCallback callback = m_control.createMock("Callback", ISocketChannelCallback.class);
		int port = getFreePort();
		InetSocketAddress address = new InetSocketAddress("239.255.255.250", port);
		Collection<NetworkInterface> inets = getInterface();

		Capture<byte[]> captureBytes = new Capture<>(CaptureType.ALL);
		Capture<InetSocketAddress> captureAddress = new Capture<>(CaptureType.ALL);
		callback.handleRead(EasyMock.capture(captureBytes), EasyMock.capture(captureAddress));
		callback.close();
		m_control.replay();
		try (DatagramListenerChannelInstance instance = m_manager.listenMultigram(address, inets, callback))
		{
			Thread.sleep(30); // sleep to let the socket to ready
			try (MulticastSocket ds = new MulticastSocket())
			{
				byte[] bytes = "Test".getBytes();
				DatagramPacket p = new DatagramPacket(bytes, bytes.length);
				p.setSocketAddress(address);
				ds.send(p);
				waitCapture(captureAddress);
			}
		}
		m_control.verify();
		assertTrue(Arrays.equals("Test".getBytes(), captureBytes.getValue()));

	}

	@Test
	public void testSendDatagram() throws Exception
	{
		int port = getFreePort();
		InetSocketAddress address = new InetSocketAddress(m_address, port);
		ISocketChannelCallback callback = m_control.createMock("Callback", ISocketChannelCallback.class);
		Capture<byte[]> captureBytes = new Capture<>(CaptureType.ALL);
		Capture<InetSocketAddress> captureAddress = new Capture<>(CaptureType.ALL);
		callback.handleRead(EasyMock.capture(captureBytes), EasyMock.capture(captureAddress));
		callback.close();

		m_control.replay();
		try (DatagramListenerChannelInstance instance = m_manager.listenDatagram(address, callback))
		{
			m_manager.sendDatagram(address, "Test".getBytes());
			waitCapture(captureBytes);
		}
		m_control.verify();
		assertTrue(Arrays.equals("Test".getBytes(), captureBytes.getValue()));

	}

	@Test
	public void testSendLargeDatagram() throws Exception
	{
		int port = getFreePort();
		InetSocketAddress address = new InetSocketAddress(m_address, port);
		ISocketChannelCallback callback = m_control.createMock("Callback", ISocketChannelCallback.class);
		Capture<byte[]> captureBytes = new Capture<byte[]>(CaptureType.ALL);
		Capture<InetSocketAddress> captureAddress = new Capture<InetSocketAddress>(CaptureType.ALL);
		callback.handleRead(EasyMock.capture(captureBytes), EasyMock.capture(captureAddress));
		EasyMock.expectLastCall().atLeastOnce();
		callback.close();
		m_control.replay();
		try (DatagramListenerChannelInstance instance = m_manager.listenDatagram(address, callback))
		{
			byte[] b = new byte[(2 << 15) - (2 << 5)];
			for (int i = 0; i < 20; i++)
				m_manager.sendDatagram(address, b);

			waitCapture(captureBytes);
		}
		m_control.verify();
	}

	@Test
	public void testSendUDPViaInstance() throws Exception
	{
		int port = getFreePort();
		InetSocketAddress address = new InetSocketAddress(m_address, port);
		ISocketChannelCallback callback = m_control.createMock("Callback", ISocketChannelCallback.class);
		Capture<byte[]> captureBytes = new Capture<>(CaptureType.ALL);
		Capture<InetSocketAddress> captureAddress = new Capture<>(CaptureType.ALL);
		callback.handleRead(EasyMock.capture(captureBytes), EasyMock.capture(captureAddress));
		callback.close();

		m_control.replay();
		try (DatagramListenerChannelInstance instance = m_manager.listenDatagram(address, callback))
		{
			Thread.sleep(30);
			byte[] b = "Test".getBytes();
			instance.send(ByteBuffer.wrap(b), address);

			waitCapture(captureBytes);
		}
		m_control.verify();
		assertTrue(Arrays.equals("Test".getBytes(), captureBytes.getValue()));
	}

	@Test
	public void testWriteTCP() throws Exception
	{
		int port = getFreePort();
		InetSocketAddress address = new InetSocketAddress(m_address, port);

		ITCPSocketChannelCallback callback = m_control.createMock("Callback", ITCPSocketChannelCallback.class);
		final Capture<TCPSocketChannelInstance> captureInstance = new Capture<>(CaptureType.ALL);
		final Capture<byte[]> captureBytes = new Capture<>(CaptureType.ALL);
		callback.connectionOpen(EasyMock.capture(captureInstance));
		callback.connectionClose(EasyMock.capture(captureInstance));
		callback.close();

		WritableByteChannel channel = null;
		m_control.replay();
		try (final ServerSocket s = new ServerSocket(port))
		{
			Thread t = new Thread() {
				@Override
				public void run()
				{
					try
					{
						try (Socket sA = s.accept())
						{
							waitCapture(captureInstance);
							byte[] b = new byte[100];
							InputStream inputStream = sA.getInputStream();
							LOGGER.trace("read {} bytes", inputStream.read(b));
							captureBytes.setValue(b);
						}
					}
					catch (Exception e)
					{
						LOGGER.error("Exception", e);
					}
				};
			};
			t.start();
			try (WritableByteChannel instance = m_manager.connect(address, callback))
			{
				channel = instance;
				waitCapture(captureInstance);
				byte[] b = "Test".getBytes();
				instance.write(ByteBuffer.wrap(b));
				waitCapture(captureBytes);
				waitCapture(captureInstance, 2);
			}
			t.join(1000);
		}
		m_control.verify();
		assertEquals(Arrays.asList(channel, channel), captureInstance.getValues());
	}

	@Test
	public void testTCPRead() throws Exception
	{
		int port = getFreePort();
		InetSocketAddress address = new InetSocketAddress(m_address, port);
		ITCPSocketChannelCallback callback = m_control.createMock("Callback", ITCPSocketChannelCallback.class);
		final Capture<TCPSocketChannelInstance> captureInstance = new Capture<>(CaptureType.ALL);
		final Capture<byte[]> captureBytes = new Capture<>(CaptureType.ALL);

		callback.connectionOpen(EasyMock.capture(captureInstance));
		callback.handleRead(EasyMock.capture(captureBytes), EasyMock.isA(InetSocketAddress.class));
		callback.connectionClose(EasyMock.capture(captureInstance));
		callback.close();

		final CyclicBarrier barrierStart = new CyclicBarrier(2);

		WritableByteChannel channel = null;
		m_control.replay();
		try (final ServerSocket s = new ServerSocket(port))
		{
			Thread t = new Thread() {
				@Override
				public void run()
				{
					try
					{
						barrierStart.await(1, TimeUnit.SECONDS);
						try (Socket sA = s.accept())
						{
							waitCapture(captureInstance);
							byte[] b = "Test".getBytes();
							OutputStream outputStream = sA.getOutputStream();
							LOGGER.trace("writing bytes");
							outputStream.write(b);
							outputStream.flush();
							outputStream.close();
							waitCapture(captureBytes);
						}
					}
					catch (Exception e)
					{
						LOGGER.error("Exception", e);
					}
				};
			};
			t.start();
			barrierStart.await(1, TimeUnit.SECONDS);
			try (WritableByteChannel instance = m_manager.connect(address, callback))
			{
				channel = instance;
				waitCapture(captureBytes);
				waitCapture(captureInstance, 2);
			}
			t.join(1000);
		}
		m_control.verify();
		assertEquals(Arrays.asList(channel, channel), captureInstance.getValues());
		assertTrue(Arrays.equals("Test".getBytes(), captureBytes.getValue()));
	}

	private Collection<NetworkInterface> getInterface() throws SocketException
	{
		// TODO IPv6 support
		Class<? extends InetAddress> clazz = Inet4Address.class;
		Collection<NetworkInterface> inets = new ArrayList<>();
		for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();)
		{
			NetworkInterface inet = e.nextElement();

			for (Enumeration<InetAddress> addresses = inet.getInetAddresses(); addresses.hasMoreElements();)
			{
				InetAddress address = addresses.nextElement();
				if (clazz.isInstance(address) && !address.isLoopbackAddress())
					inets.add(inet);
			}
		}
		assertFalse(inets.isEmpty());
		return inets;
	}

	private static final Random RANDOM = new Random();

	private int getFreePort()
	{
		while (true)
		{
			int port = RANDOM.nextInt(2 << 16 - 1000) + 1000;

			for (int i = 0; i < 100 && port < 2 << 16; i++, port++)
			{
				InetSocketAddress address = new InetSocketAddress(m_address, port);
				try
				{
					DatagramSocket ds = new DatagramSocket(port);
					ds.setReuseAddress(true);
					ds.disconnect();
					ds.close();
				}
				catch (SocketException e)
				{
					LOGGER.trace("UDP {}", port, e);
					continue;
				}
				try (ServerSocket s = new ServerSocket())
				{
					s.setReuseAddress(true);
					s.bind(address);
				}
				catch (IOException e)
				{
					LOGGER.trace("TCP Listen {}", port, e);
					continue;
				}
				LOGGER.trace("Using port {}", port);
				return port;
			}
		}
	}

	private static class ThreadCaptureAnswer<T> implements IAnswer<T>
	{
		private final IAnswer<T> m_answer;
		private final Capture<Thread> m_threads = new Capture<>(CaptureType.ALL);

		public ThreadCaptureAnswer(IAnswer<T> answer)
		{
			m_answer = answer;
		}

		@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
		public ThreadCaptureAnswer(Throwable t)
		{
			this((IAnswer) Result.createThrowResult(t));
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public ThreadCaptureAnswer(T t)
		{
			this((IAnswer) Result.createReturnResult(t));
		}

		public ThreadCaptureAnswer()
		{
			this((T) null);
		}

		@Override
		public T answer() throws Throwable
		{
			m_threads.setValue(Thread.currentThread());
			return m_answer.answer();
		}

		public Capture<Thread> getThreads()
		{
			return m_threads;
		}
	}
}
