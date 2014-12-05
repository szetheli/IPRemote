/**
 *
 * DatagramListenerChannelInstance.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.net.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DatagramListenerChannelInstance wraps a channel for use with UDP
 * 
 * @author Sven Zethelius
 * 
 */
public class DatagramListenerChannelInstance extends SocketChannelInstance<DatagramChannel>
{

	private static final Logger LOGGER = LoggerFactory.getLogger(DatagramListenerChannelInstance.class);
	private Collection<SocketAddress> m_loopbackAddresses;

	public DatagramListenerChannelInstance(
			ISocketChannelCallback callback, 
			DatagramChannel channel, 
			Collection<SocketAddress> loopbackAddresses)
	{
		super(callback);
		m_loopbackAddresses = loopbackAddresses;
		setChannel(channel);
	}

	@Override
	void setKey(SelectionKey key)
	{
		super.setKey(key);
	}

	@Override
	protected int processRead(ByteBuffer b) throws IOException
	{
		DatagramChannel channel = getChannel();
		if (channel == null)
			return 0;

		InetSocketAddress address = (InetSocketAddress) channel.receive(b);
		if (address != null && !m_loopbackAddresses.contains(address))
			doRead(b, address);
		b.clear();
		return 0;
	}

	/**
	 * Send a UDP packet to the address specified, using the local port this instance is bound to.
	 * 
	 * @param b
	 * @param address
	 * @throws IOException
	 */
	public void send(ByteBuffer b, SocketAddress address) throws IOException
	{
		DatagramChannel channel = getChannel();
		if (channel == null)
			throw new ClosedChannelException();
		if (LOGGER.isTraceEnabled())
			LOGGER.trace("Sending from {} to {}:{}", this, address,
					new String(b.array(), b.position(), b.remaining()).getBytes());

		channel.send(b, address);
		if (b.remaining() > 0)
			throw new IOException("Unable to send all bytes.  Remaining:" + b.remaining());
	}


	@Override
	public String toString()
	{
		DatagramChannel channel = getChannel();
		if (channel == null)
			return getClass().getSimpleName();
		return channel.socket().getLocalSocketAddress().toString();
	}
}
