/**
 * 
 */
package svenz.remote.android;

import java.io.IOException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.Utilities;
import svenz.remote.device.DeviceGroupRegistry;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.protocol.ssdp.SSDPManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * @author Sven Zethelius
 *
 */
public class ConnectionReceiver extends BroadcastReceiver
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionReceiver.class);
	private SocketChannelManager m_socketChannelManager;
	private SSDPManager m_ssdpManager;
	private DeviceGroupRegistry m_deviceGroupRegistry;

	public void setDeviceGroupRegistry(DeviceGroupRegistry deviceGroupRegistry)
	{
		m_deviceGroupRegistry = deviceGroupRegistry;
	}

	public void setSocketChannelManager(SocketChannelManager socketChannelManager)
	{
		m_socketChannelManager = socketChannelManager;
	}

	public void setSSDPManager(SSDPManager ssdpManager)
	{
		m_ssdpManager = ssdpManager;
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		if (isValidConnection(wifiManager))
		{
			try
			{
				m_socketChannelManager.open();
				m_ssdpManager.open();
				m_deviceGroupRegistry.open();
			}
			catch (IOException e)
			{
				LOGGER.error("Error opening registry", e);
			}
		}
		else
		{
			Utilities.safeClose(m_ssdpManager);
			Utilities.safeClose(m_socketChannelManager);
		}
	}

	private boolean isValidConnection(android.net.wifi.WifiManager wifiManager)
	{
		if (!wifiManager.isWifiEnabled())
			return false;

		WifiInfo connectionInfo = wifiManager.getConnectionInfo();
		if (connectionInfo == null)
			return false;
		String ssid = connectionInfo.getSSID();
		if (ssid == null || !ssid.toLowerCase(Locale.US).contains("svenz"))
			return false;

		return true;
	}



}
