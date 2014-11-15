/**
 * 
 */
package svenz.remote.net.protocol.ssdp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import svenz.remote.common.utilities.Utilities;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;
import svenz.remote.net.protocol.ssdp.jaxb.Device;

/**
 * @author Sven Zethelius
 *
 */
public class CompositeDeviceListener<T> implements IDeviceListener<List<T>>
{
	private final Collection<IDeviceListener<T>> m_listeners;

	public CompositeDeviceListener(Collection<IDeviceListener<T>> listeners)
	{
		super();
		m_listeners = listeners;
	}

	@Override
	public void close() throws IOException
	{
		for (IDeviceListener<T> listener : m_listeners)
			Utilities.safeClose(listener);
	}

	@Override
	public List<T> deviceAdded(Device device, InetSocketAddress remoteAddress)
	{
		List<T> out = new ArrayList<T>(m_listeners.size());
		boolean register = false;
		for (IDeviceListener<T> listener : m_listeners)
		{
			T t = listener.deviceAdded(device, remoteAddress);
			out.add(t);
			register |= t != null;
		}
		return register ? out : null;
	}

	@Override
	public void deviceRemoved(List<T> handle)
	{
		Iterator<T> iter = handle.iterator();
		for (IDeviceListener<T> listener : m_listeners)
		{
			T t = iter.next();
			if (t != null)
			{
				listener.deviceRemoved(t);
			}
		}
	}

}
