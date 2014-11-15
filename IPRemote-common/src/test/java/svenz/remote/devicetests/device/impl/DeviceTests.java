/**
 * 
 */
package svenz.remote.devicetests.device.impl;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import svenz.remote.device.impl.pioneer.PlayerBDP150Device;
import svenz.remote.device.impl.pioneer.ReceiverVSX1123Device;
import svenz.remote.device.impl.sharp.TVAquas60LE650Device;
import svenz.remote.device.ipremote.AbstractDevice;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.protocol.ssdp.LoggingDeviceListener;
import svenz.remote.net.protocol.ssdp.SSDPManager;

/**
 * @author Sven Zethelius
 *
 */
public class DeviceTests
{
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception
	{
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);
		executor.setKeepAliveTime(1, TimeUnit.MINUTES);
		executor.allowCoreThreadTimeOut(true);

		SocketChannelManager channelManager = new SocketChannelManager();
		SSDPManager ssdpManager = new SSDPManager(channelManager, executor);
		ssdpManager.addDeviceListener(new LoggingDeviceListener());

		AbstractDevice device;
		if ("Receiver".equals(args[0]))
			device = new ReceiverVSX1123Device();
		else if ("Player".equals(args[0]))
		{
			device = new PlayerBDP150Device();
			((PlayerBDP150Device) device).setExecutor(executor);
		}
		else if ("TV".equals(args[0]))
			device = new TVAquas60LE650Device();
		else
			throw new IllegalArgumentException();

		device.setChannelManager(channelManager);
		device.setSSDPManager(ssdpManager);

		channelManager.open();
		ssdpManager.open();

		Thread.sleep(100000);
		// TODO what was I testing? Just that the thing starts w/o error?

	}
}
