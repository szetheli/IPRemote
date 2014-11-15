/**
 * MatchingDeviceListener.java
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

import java.net.InetSocketAddress;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;
import svenz.remote.net.protocol.ssdp.jaxb.Device;

/**
 * @author Sven Zethelius
 * 
 */
public class MatchingDeviceListener<T> extends WrapperDeviceListener<T>
{
	private String m_modelName; // BDP-150/UCXESM | VSX-1123/CUXESM
	private String m_friendlyName; // BDP-150 | VSX-1123
	private String m_deviceType;
	// urn:pioneer-co-jp:device:PioControlServer:1
	// urn:schemas-upnp-org:device:MediaRenderer:1

	public MatchingDeviceListener(IDeviceListener<T> listener)
	{
		super(listener);
	}

	public void setDeviceType(String deviceType)
	{
		m_deviceType = deviceType;
	}

	public void setModelName(String modelName)
	{
		m_modelName = modelName;
	}

	public void setFriendlyName(String friendlyName)
	{
		m_friendlyName = friendlyName;
	}

	private boolean matches(Device device)
	{
		return nullEquals(m_deviceType, device.getDeviceType()) 
				&& nullEquals(m_friendlyName, device.getFriendlyName())
				&& nullEquals(m_modelName, device.getModelName());
	}

	private boolean nullEquals(Object o1, Object o2)
	{
		return o1 == null || o1.equals(o2);
	}

	@Override
	public T deviceAdded(Device device, InetSocketAddress address)
	{
		if (!matches(device))
			return null;
		return super.deviceAdded(device, address);
	}
}
