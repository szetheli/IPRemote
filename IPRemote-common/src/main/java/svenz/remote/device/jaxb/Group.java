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

// @javax.xml.bind.annotation.XmlRootElement(name = "Group")
@org.simpleframework.xml.Root(name = "Group", strict = true)
public class Group
{
	@org.simpleframework.xml.Attribute(name = "location", required = true)
	private String m_location;
	@org.simpleframework.xml.Attribute(name = "activity", required = true)
	private String m_activity;

	@org.simpleframework.xml.Element(name = "Playable", required = false)
	private DeviceReference m_playable;
	@org.simpleframework.xml.Element(name = "Sound", required = false)
	private DeviceReference m_sound;
	
	@org.simpleframework.xml.ElementList(inline = true, entry = "Menu", required = false)
	private List<MenuReference> m_menuReferences = new ArrayList<MenuReference>();

	@org.simpleframework.xml.ElementList(inline = true, entry = "Select", required = false)
	private List<SelectableReference> m_selectableReferences = new ArrayList<SelectableReference>();

	public String getLocation()
	{
		return m_location;
	}

	// @javax.xml.bind.annotation.XmlAttribute(name = "location")
	public void setLocation(String location)
	{
		m_location = location;
	}

	public String getActivity()
	{
		return m_activity;
	}

	// @javax.xml.bind.annotation.XmlAttribute(name = "activity")
	public void setActivity(String activity)
	{
		m_activity = activity;
	}

	public DeviceReference getPlayable()
	{
		return m_playable;
	}

	// @javax.xml.bind.annotation.XmlElement(name = "Playable")
	public void setPlayable(DeviceReference playable)
	{
		m_playable = playable;
	}

	public DeviceReference getSound()
	{
		return m_sound;
	}

	// @javax.xml.bind.annotation.XmlElement(name = "Sound")
	public void setSound(DeviceReference sound)
	{
		m_sound = sound;
	}

	// @javax.xml.bind.annotation.XmlElement(name = "Menu")
	public List<MenuReference> getMenuReferences()
	{
		return m_menuReferences;
	}

	public void setMenuReferences(List<MenuReference> menuReferences)
	{
		m_menuReferences = menuReferences;
	}

	// @javax.xml.bind.annotation.XmlElement(name = "Select")
	public List<SelectableReference> getSelectableReferences()
	{
		return m_selectableReferences;
	}

	public void setSelectableReferences(List<SelectableReference> selectableReferences)
	{
		m_selectableReferences = selectableReferences;
	}
}
