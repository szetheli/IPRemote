/**
 * 
 */
package svenz.remote.net.protocol.ssdp;

import java.io.IOException;
import java.net.InetSocketAddress;
import svenz.remote.net.nio.ITCPSocketChannelCallback;
import svenz.remote.net.nio.TCPSocketChannelInstance;

/**
 * 
 * 
 * @author Sven Zethelius
 * 
 */
public class SSDPChannelCallback implements ITCPSocketChannelCallback
{
	private final SSDPManager m_manager;
	private final String m_udn;

	public SSDPChannelCallback(SSDPManager manager, String udn)
	{
		super();
		m_manager = manager;
		m_udn = udn;
	}

	@Override
	public void handleRead(byte[] b, InetSocketAddress address) throws IOException
	{
		// do nothing
	}

	@Override
	public void close()
	{
		m_manager.notifyDeviceRemoved(m_udn);
	}

	@Override
	public void connectionOpen(TCPSocketChannelInstance instance)
	{
		// do nothing
	}

	@Override
	public void connectionClose(TCPSocketChannelInstance instance)
	{
		close();
	}

	@Override
	public void connectionFailed(TCPSocketChannelInstance instance)
	{
		close();
	}

}
