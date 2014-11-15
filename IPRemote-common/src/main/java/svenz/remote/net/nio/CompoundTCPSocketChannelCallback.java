/**
 * 
 */
package svenz.remote.net.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import svenz.remote.common.utilities.Utilities;

/**
 * @author Sven Zethelius
 *
 */
public class CompoundTCPSocketChannelCallback implements ITCPSocketChannelCallback
{
	private final Collection<? extends ITCPSocketChannelCallback> m_callbacks;

	public CompoundTCPSocketChannelCallback(Collection<? extends ITCPSocketChannelCallback> callbacks)
	{
		m_callbacks = callbacks;
	}

	@Override
	public void handleRead(byte[] b, InetSocketAddress address) throws IOException
	{
		for (ITCPSocketChannelCallback callback : m_callbacks)
			callback.handleRead(b, address);
	}

	@Override
	public void close()
	{
		for (ITCPSocketChannelCallback callback : m_callbacks)
			Utilities.safeClose(callback);
	}

	@Override
	public void connectionOpen(TCPSocketChannelInstance instance)
	{
		for (ITCPSocketChannelCallback callback : m_callbacks)
			callback.connectionOpen(instance);
	}

	@Override
	public void connectionClose(TCPSocketChannelInstance instance)
	{
		for (ITCPSocketChannelCallback callback : m_callbacks)
			callback.connectionClose(instance);
	}


	@Override
	public void connectionFailed(TCPSocketChannelInstance instance)
	{
		for (ITCPSocketChannelCallback callback : m_callbacks)
			callback.connectionFailed(instance);
	}

}
