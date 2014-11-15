/**
 * 
 */
package svenz.remote.net.protocol.ssdp;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;
import svenz.remote.net.protocol.ssdp.SSDPManager.SSDPSearchRequest;
import svenz.remote.net.protocol.ssdp.jaxb.Device;

/**
 * @author Sven Zethelius
 *
 */
public class SearchDeviceListener implements IDeviceListener<String>
{
	private final SSDPManager m_manager;
	private final Map<String, Long> m_lastSearch = new HashMap<String, Long>();

	public SearchDeviceListener(SSDPManager manager)
	{
		m_manager = manager;
	}

	@Override
	public void close()
	{
		synchronized (this)
		{
			m_lastSearch.clear();
		}
	}

	@Override
	public String deviceAdded(Device device, InetSocketAddress remoteAddress)
	{
		return device.getUDN();
	}

	@Override
	public void deviceRemoved(String udn)
	{
		synchronized (this)
		{
			long now = System.currentTimeMillis();
			Long lastSearch = m_lastSearch.get(udn);
			// limit to once per minute in case we are in a condition loop for some reason
			if (lastSearch != null && lastSearch + 60 * 1000 > now)
				return;
			m_lastSearch.put(udn, now);
		}
		// issue a search for this device in case its a local failure reason for us disconnecting
		SSDPSearchRequest request = new SSDPSearchRequest(udn, 3);
		m_manager.search(request);
	}

}
