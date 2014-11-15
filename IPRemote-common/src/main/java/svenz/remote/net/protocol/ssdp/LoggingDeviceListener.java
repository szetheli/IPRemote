/**
 * LoggingDeviceListener.java
 * Copyright 2013, Sven Zethelius
 * 
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package svenz.remote.net.protocol.ssdp;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;
import svenz.remote.net.protocol.ssdp.jaxb.Device;

/**
 * @author Sven Zethelius
 *
 */
public class LoggingDeviceListener implements IDeviceListener<Device>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingDeviceListener.class);

	@Override
	public Device deviceAdded(Device device, InetSocketAddress address)
	{
		LOGGER.info("DeviceAdded: type:{}\tfriendlyname:{}\tmodel:{}\taddress:{}", device.getDeviceType(),
				device.getFriendlyName(), device.getModelName(), address);
		return device;
	}

	@Override
	public void deviceRemoved(Device device)
	{
		LOGGER.info("DeviceRemoved: type:" + device.getDeviceType() + "\tfriendlyname:" + device.getFriendlyName()
				+ "\tmodel:" + device.getModelName());
	}

	@Override
	public void close() throws IOException
	{
		LOGGER.info("Closed");
	}

}
