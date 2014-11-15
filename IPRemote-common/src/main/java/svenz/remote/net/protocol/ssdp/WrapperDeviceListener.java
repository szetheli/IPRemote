/**
 * 
 */
package svenz.remote.net.protocol.ssdp;

import java.io.IOException;
import java.net.InetSocketAddress;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;
import svenz.remote.net.protocol.ssdp.jaxb.Device;

/**
 * A naive wrapper to be overridden
 * 
 * @author Sven Zethelius
 * 
 */
public class WrapperDeviceListener<T> implements IDeviceListener<T>
{
	private final IDeviceListener<T> m_listener;

	public WrapperDeviceListener(IDeviceListener<T> listener)
	{
		super();
		m_listener = listener;
	}

	@Override
	public void close() throws IOException
	{
		m_listener.close();
	}

	@Override
	public T deviceAdded(Device device, InetSocketAddress remoteAddress)
	{
		return m_listener.deviceAdded(device, remoteAddress);
	}

	@Override
	public void deviceRemoved(T handle)
	{
		m_listener.deviceRemoved(handle);
	}

}
