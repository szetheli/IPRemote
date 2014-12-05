/**
 *
 * SocketChannelManager.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.net.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.Utilities;

/**
 * SocketChannelManager wraps NIO selector to create a managed object per
 * connection
 * 
 * @author Sven Zethelius
 * 
 */
public class SocketChannelManager implements Closeable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SocketChannelManager.class);
	private static final boolean JDK7_NIO = checkNIO();
	private final Queue<Callable<? extends SelectableChannel>> m_pending =
			new ConcurrentLinkedQueue<Callable<? extends SelectableChannel>>();
	private Selector m_selector;
	private ScheduledExecutorService m_executor;
	private final Map<SocketAddress, Boolean> m_loopbackAddress = new ConcurrentHashMap<SocketAddress, Boolean>();


	/**
	 * Future for controlling the selector thread
	 */
	private Future<?> m_future;

	/**
	 * Channel for sending messages, unbound so must provide address
	 */
	private DatagramChannel m_datagram;

	public void setExecutor(ScheduledExecutorService executor)
	{
		m_executor = executor;
	}

	Collection<SocketAddress> getLoopbackAddress()
	{
		return m_loopbackAddress.keySet();
	}

	/**
	 * Check if JDK 7 NIO is available
	 * 
	 * @return
	 */
	private static boolean checkNIO()
	{
		try
		{
			Class.forName("java.net.ProtocolFamily");
			return true;
		}
		catch (ClassNotFoundException e)
		{
			return false;
		}
	}

	/**
	 * Prepare the {@link SocketChannelManager} for operation.
	 * 
	 * @throws IOException
	 */
	public void open() throws IOException
	{
		synchronized (this)
		{
			if (m_selector != null)
				return;
			m_selector = Selector.open();
			m_future = m_executor.scheduleAtFixedRate(new SelectorThread(), 0, 1, TimeUnit.MICROSECONDS);

			m_datagram = DatagramChannel.open();
			configureChannel(m_datagram);
		}
	}

	@Override
	public void close()
	{
		synchronized (this)
		{
			if(m_selector == null)
				return;
			m_future.cancel(false);
			m_future = null;
			for (SelectionKey key : new ArrayList<SelectionKey>(m_selector.keys()))
			{
				Utilities.safeClose((SocketChannelInstance<?>) key.attachment());
			}

			wakeSelector();
			Utilities.safeClose(m_selector);
			m_selector = null;
			Utilities.safeClose(m_datagram);
			m_datagram = null;
		}
	}

	private void configureChannel(DatagramChannel channel) throws IOException
	{
		channel.socket().setReuseAddress(true);
		channel.configureBlocking(false);
	}

	private DatagramChannel getBindableChannel(InetAddress address) throws IOException
	{
		return JDK7_NIO ? DatagramChannel.open(getProtocolFamily(address)) : DatagramChannel.open();
	}

	/**
	 * Listen to a multigram socket (Multicast UDP)
	 * 
	 * @param address
	 * @param callback
	 * @return
	 * @throws IOException
	 */
	public DatagramListenerChannelInstance listenMultigram(InetSocketAddress address,
			Collection<NetworkInterface> interfaces,
			ISocketChannelCallback callback) throws IOException
	{
		if(!JDK7_NIO)
			return null; // TODO listen to multicast via dedicated socket receive threads per instance.
		
		InetAddress iaddress = address.getAddress();
		DatagramChannel channel = getBindableChannel(iaddress);
		try
		{
			configureChannel(channel);
			channel.socket().bind(new InetSocketAddress(address.getPort())); // can't use JDK7 channel.bind
			
			for (NetworkInterface inet : interfaces)
				channel.join(iaddress, inet);
	
			return listenDatagram(channel, callback);
		}
		catch (IOException e)
		{
			channel.disconnect();
			Utilities.safeClose(channel);
			throw e;
		}
	}

	/**
	 * Listen to a datagram socket (UDP)
	 * 
	 * @param address
	 * @param callback
	 * @return
	 * @throws IOException
	 */
	public DatagramListenerChannelInstance listenDatagram(InetSocketAddress address, ISocketChannelCallback callback)
			throws IOException
	{
		DatagramChannel channel = getBindableChannel(address.getAddress());
		try
		{
			configureChannel(channel);
			channel.socket().bind(address); // can't use JDK7 channel.bind
			return listenDatagram(channel, callback);
		}
		catch (IOException e)
		{
			channel.disconnect();
			Utilities.safeClose(channel);
			throw e;
		}
	}

	private DatagramListenerChannelInstance listenDatagram(final DatagramChannel channel,
			final ISocketChannelCallback callback)
	{
		final DatagramListenerChannelInstance instance = new DatagramListenerChannelInstance(callback, channel, m_loopbackAddress.keySet());
		Callable<DatagramChannel> c = new Callable<DatagramChannel>()
			{
				@Override
				public DatagramChannel call() throws Exception
				{
					try
					{
						m_loopbackAddress.put(channel.getLocalAddress(), Boolean.TRUE);
						instance.setKey(channel.register(m_selector, SelectionKey.OP_READ, instance));
					}
					catch (Exception e)
					{
						LOGGER.error("Unable to establish socket {}", instance);
						Utilities.safeClose(instance);
						return null;
					}
					return channel;
				}
			};
		if (!m_pending.add(c))
			throw new IllegalStateException("Unable to enqueue pending listen operation");
		wakeSelector();
		return instance;
	}

	private void wakeSelector()
	{
		synchronized (this)
		{
			if (m_selector != null)
				m_selector.wakeup();
		}
	}

	private ProtocolFamily getProtocolFamily(InetAddress address)
	{
		if (address instanceof Inet4Address)
			return StandardProtocolFamily.INET;
		if (address instanceof Inet6Address)
			return StandardProtocolFamily.INET6;
		throw new IllegalStateException("Unknown address");
	}

	/**
	 * Send a UDP packet from an ephemeral address
	 * 
	 * @param address
	 * @param b
	 * @throws IOException
	 */
	public void sendDatagram(InetSocketAddress address, byte[] b) throws IOException
	{
		LOGGER.trace("Sending to {}: {}", address, b);
		// TODO test IPV6
		ByteBuffer bb = ByteBuffer.wrap(b);
		m_datagram.send(bb, address);
		if (bb.remaining() > 0)
			throw new IOException("Unable to write all bytes on UDP send.  Sent " + (b.length - bb.remaining())
					+ " of " + b.length);
	}
	
	public WritableByteChannel connect(InetSocketAddress address, ITCPSocketChannelCallback callback)
			throws IOException
	{
		return connect(address, 0, callback);
	}

	public WritableByteChannel connect(InetSocketAddress address, int timeout, ITCPSocketChannelCallback callback)
			throws IOException
	{
		TCPSocketChannelInstance instance = new TCPSocketChannelInstance(address, callback);
		connect(instance, address, timeout);
		return instance;
	}
	
	void connect(final TCPSocketChannelInstance instance, final InetSocketAddress address, int timeout)
			throws IOException
	{
		LOGGER.trace("Connecting to {}", address);
		final SocketChannel channel = SocketChannel.open();
		try
		{
			channel.configureBlocking(false);
			channel.connect(address);
		}
		catch (IOException e)
		{
			Utilities.safeClose(channel);
			throw e;
		}
		instance.setChannel(channel);
		Callable<SocketChannel> c = new Callable<SocketChannel>() {
			@Override
			public SocketChannel call() throws Exception
			{
				try
				{
					instance.setKey(channel.register(m_selector, SelectionKey.OP_CONNECT, instance));
				}
				catch (Exception e)
				{
					LOGGER.error("Unable to connect to {}", address, e);
					Utilities.safeClose(instance);
					return null;
				}
				return channel;
			}
		};
		m_pending.add(c);
		if (timeout > 0)
		{
			m_executor.schedule(new Runnable() {
				@Override
				public void run()
				{
					if (channel.isConnectionPending())
						Utilities.safeClose(instance);
				}
			}, timeout, TimeUnit.MILLISECONDS);
		}
		wakeSelector();
	}
	
	private void registerPending(Selector selector) throws Exception
	{
		for (Iterator<SelectionKey> iter = selector.keys().iterator(); iter.hasNext();)
		{
			SelectionKey key = iter.next();
			SocketChannelInstance<?> instance = (SocketChannelInstance<?>) key.attachment();
			if (!key.isValid())
			{
				Utilities.safeClose(instance); // just in case it wasn't already closed
				continue;
			}

			int opsExisting = key.interestOps();
			int ops = instance.getPendingOperations() | opsExisting;
			if (ops != opsExisting)
				key.interestOps(ops);
		}

		for (Iterator<Callable<? extends SelectableChannel>> iter = m_pending.iterator(); iter.hasNext();)
		{
			try
			{
				iter.next().call(); // TODO need channel?
			}
			catch (Exception e)
			{
				LOGGER.error("Unable to handle pending socket", e);
			}
			finally
			{
				iter.remove();
			}
		}
	}

	private class SelectorThread implements Runnable
	{
		private final ByteBuffer m_readBuffer = ByteBuffer.allocate(1024);


		@Override
		public void run()
		{
			try
			{
				Selector selector = m_selector;
				registerPending(selector);
				int selected = selector.select();
				if(selected > 0)
					LOGGER.trace("{} channels ready.", selected);
				for (Iterator<SelectionKey> iter = selector.selectedKeys().iterator(); iter.hasNext();)
				{
					SelectionKey key = iter.next();
					iter.remove();
					handleChannel(key);
				}
			}
			catch (ClosedSelectorException e)
			{
				Utilities.safeClose(SocketChannelManager.this);
			}
			catch (Exception e)
			{
				LOGGER.error("Unhandled exception in selector thread", e);
			}
		}

		private void handleChannel(SelectionKey key)
		{
			SocketChannelInstance<?> instance = (SocketChannelInstance<?>) key.attachment();
			try
			{
				if (!key.isValid() || !key.channel().isOpen())
				{
					Utilities.safeClose(instance);
				}
				else if(key.isReadable())
				{
					instance.processRead(m_readBuffer);
				}
				else
				{
					instance.process();
				}
			}
			catch (CancelledKeyException e)
			{
				LOGGER.trace("Closing channel {}", key.attachment(), e);
				Utilities.safeClose(instance);
			}
			catch (ClosedChannelException e)
			{
				LOGGER.trace("Closing channel {}", key.attachment(), e);
				Utilities.safeClose(instance);
			}
			catch (ConnectException e)
			{
				LOGGER.error("Unable to connect to " + key.attachment(), e);
				Utilities.safeClose(instance);
			}
			catch (IOException e)
			{
				String op = "Unknown";
				try
				{
					if (key.isConnectable())
						op = "connect";
					else if (key.isReadable())
						op = "read";
					else if (key.isWritable())
						op = "write";
				}
				catch(Exception e2)
				{
					LOGGER.error("Secondary exception from key " + key.attachment(), e);
				}
				LOGGER.error("Error processing " + key.attachment() + " for op:" + op, e);
				Utilities.safeClose(instance);
			}
		}
	}


}
