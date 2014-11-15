/**
 * 
 */
package svenz.remote.device.jaxb;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sven Zethelius
 *
 */
// @javax.xml.bind.annotation.XmlRootElement(name = "DeviceGroups")
@org.simpleframework.xml.Root(name = "DeviceGroups", strict = true)
public class DeviceGroups
{
	@org.simpleframework.xml.ElementList(name = "Devices", entry = "Device", required = true)
	private List<Device> m_devices = new ArrayList<Device>(4);
	@org.simpleframework.xml.ElementList(name = "Groups", entry = "Group", required = true)
	private List<Group> m_groups = new ArrayList<Group>();

	// @javax.xml.bind.annotation.XmlElementWrapper(name = "Devices")
	// @javax.xml.bind.annotation.XmlElement(name = "Device")
	public List<Device> getDevices()
	{
		if (m_devices == null)
			m_devices = new ArrayList<Device>();
		return m_devices;
	}

	public void setDevices(List<Device> devices)
	{
		m_devices = devices;
	}

	// @javax.xml.bind.annotation.XmlElementWrapper(name = "Groups")
	// @javax.xml.bind.annotation.XmlElement(name = "Group")
	public List<Group> getGroups()
	{
		if (m_groups == null)
			m_groups = new ArrayList<Group>();
		return m_groups;
	}

	public void setGroups(List<Group> groups)
	{
		m_groups = groups;
	}
}
