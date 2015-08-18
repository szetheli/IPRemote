/**
 *
 * SocketChannelInstance.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.net.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.Utilities;

/**
 * TODO szetheli Describe SocketChannelInstance
 * 
 * @author Sven Zethelius
 *
 */
public abstract class SocketChannelInstance<T extends SelectableChannel> implements Closeable, Channel
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SocketChannelInstance.class);
	private static final ISocketChannelCallback DEFAULT_CALLBACK = 
			new CompoundTCPSocketChannelCallback(Collections.<ITCPSocketChannelCallback>emptyList());

	protected final Queue<ByteBuffer> m_pendingWrites = new ConcurrentLinkedQueue<ByteBuffer>();
	private SelectionKey m_key;
	private final AtomicReference<T> m_channel = new AtomicReference<T>();
	protected final ISocketChannelCallback m_callback;

	SocketChannelInstance(ISocketChannelCallback callback)
	{
		m_callback = callback != null ? callback : DEFAULT_CALLBACK;
	}

	void setKey(SelectionKey key)
	{
		synchronized (this)
		{
			m_key = key;
		}
	}

	SelectionKey getKey()
	{
		return m_key;
	}

	void process() throws IOException
	{
		throw new UnsupportedOperationException("Key in unknown state:" + m_key.readyOps());
	}

	protected void register() throws ClosedChannelException
	{
		m_key = m_key.channel().register(m_key.selector(), getPendingOperations(), this);
	}

	protected abstract int processRead(ByteBuffer b) throws IOException;

	protected void doRead(ByteBuffer b, InetSocketAddress address) throws IOException
	{
		int pos = b.position();
		byte[] bytes = new byte[pos];
		((ByteBuffer) b.flip()).get(bytes);

		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Read: {} from {}", bytes.length <= 100 ? bytes : bytes.length, address);

		m_callback.handleRead(bytes, address);
	}

	@Override
	public boolean isOpen()
	{
		T channel = m_channel.get();
		return channel != null && channel.isOpen();
	}

	public int getPendingOperations()
	{
		int ops = 0;
		if (m_callback != null)
			ops |= SelectionKey.OP_READ;
		if (!m_pendingWrites.isEmpty())
			ops |= SelectionKey.OP_WRITE;
		return ops;
	}

	@Override
	public void close()
	{
		T channel = m_channel.getAndSet(null);
		if (channel == null)
			return;
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Closing {}", m_key != null ? m_key.attachment() : getClass().getSimpleName());
		// if key is null, we have not finished the connect because we
		// haven't even registered!
		if (m_key != null)
			m_key.cancel();
		Utilities.safeClose(channel);
		m_pendingWrites.clear();
		Utilities.safeClose(m_callback);
	}

	protected final T getChannel()
	{
		return m_channel.get();
	}

	public void setChannel(T channel)
	{
		m_channel.set(channel);
	}

}
