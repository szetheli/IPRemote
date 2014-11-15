/**
 *
 * TCPSocketChannelInstance.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.net.nio;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.Utilities;

/**
 * TCPSocketChannelInstance provides a channel wrapper for dealing with TCP connections.
 * 
 * @author Sven Zethelius
 * 
 */
public class TCPSocketChannelInstance extends SocketChannelInstance<SocketChannel> implements WritableByteChannel
{
	private static final Logger LOGGER = LoggerFactory.getLogger(TCPSocketChannelInstance.class);
	private static final ITCPSocketChannelCallback DEFAULT_CALLBACK = 
			new CompoundTCPSocketChannelCallback(Collections.<ITCPSocketChannelCallback> emptyList());
	private final InetSocketAddress m_remoteAddress;
	private final ITCPSocketChannelCallback m_callback;
	private final AtomicReference<State> m_state = new AtomicReference<State>(State.NotConnected);

	private static enum State
	{
		NotConnected, Connected, Closed
	};
	
	TCPSocketChannelInstance(InetSocketAddress address, ITCPSocketChannelCallback callback)
	{
		super(callback != null ? callback : (callback = DEFAULT_CALLBACK));
		m_remoteAddress = address;
		m_callback = callback;
	}

	@Override
	void setKey(SelectionKey key)
	{
		super.setKey(key);
	}

	@Override
	protected int processRead(ByteBuffer b) throws IOException
	{
		SocketChannel channel = getChannel();
		if (channel == null)
		{ // already closed. close again just to make sure, but can't read.
			Utilities.safeClose(this);
			return -1;
		}
		int read = channel.read(b);
		if (read == -1)
		{
			Utilities.safeClose(this);
			return -1;
		}

		doRead(b, m_remoteAddress);
		b.clear();
		return read;
	}

	@Override
	void process() throws IOException
	{
		SelectionKey key = getKey();
		if (key.isConnectable())
		{
			SocketChannel socketChannel = getChannel();
			try
			{
				socketChannel.finishConnect();
			}
			catch (ConnectException e)
			{
				LOGGER.error("Unable to connect to {} ", key.attachment(), e);
				Utilities.safeClose(this);
				return;
			}
			register();
			LOGGER.trace("Connected to {}", m_remoteAddress);
			m_state.set(State.Connected);
			m_callback.connectionOpen(this);
		}
		else if (key.isWritable() && !m_pendingWrites.isEmpty())
		{
			processWrite();
			register();
		}
		else
		{
			super.process();
		}
	}

	void processWrite() throws IOException
	{
		SocketChannel channel = getChannel();
		if (channel == null || !channel.isConnected())
			return;
		for (Iterator<ByteBuffer> iter = m_pendingWrites.iterator(); iter.hasNext();)
		{
			ByteBuffer b = iter.next();
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("Writing {} bytes to {}", b.remaining(), m_remoteAddress);
			channel.write(b);
			if (b.remaining() > 0)
				break;
			iter.remove();
		}
	}

	public void write(byte[] b, int off, int len)
	{
		m_pendingWrites.add(ByteBuffer.wrap(b, off, len));
		SelectionKey key = getKey();
		if (key != null)
			key.selector().wakeup();
	}

	@Override
	public int write(ByteBuffer src)
	{
		byte[] b = new byte[src.remaining()];
		src.get(b);
		write(b, 0, b.length);
		return b.length;
	}

	@Override
	public void close()
	{
		State state = m_state.getAndSet(State.Closed);
		super.close();
		if (state == State.Connected)
			m_callback.connectionClose(this);
		else if (state == State.NotConnected)
			m_callback.connectionFailed(this);
	}

	@Override
	public String toString()
	{
		return m_remoteAddress.toString();
	};
}
