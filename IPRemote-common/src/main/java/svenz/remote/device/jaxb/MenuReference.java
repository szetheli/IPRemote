/**
 * 
 */
package svenz.remote.device.jaxb;


/**
 * @author Sven Zethelius
 *
 */
// @javax.xml.bind.annotation.XmlType
@org.simpleframework.xml.Root(name = "Menu", strict = true)
public class MenuReference extends DeviceReference
{
	@org.simpleframework.xml.Attribute(name = "name", required = true)
	private String m_name;

	public String getName()
	{
		return m_name;
	}

	// @javax.xml.bind.annotation.XmlAttribute(name = "name")
	public void setName(String name)
	{
		m_name = name;
	}
}
