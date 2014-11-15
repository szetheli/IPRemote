/**
 * 
 */
package svenz.remote.device.jaxb;


/**
 * @author Sven Zethelius
 *
 */
// @javax.xml.bind.annotation.XmlType
@org.simpleframework.xml.Root(name = "Select", strict = true)
public class SelectableReference extends DeviceReference
{
	@org.simpleframework.xml.Attribute(name = "value", required = true)
	private String m_value;

	public String getValue()
	{
		return m_value;
	}

	// @javax.xml.bind.annotation.XmlAttribute(name = "value")
	public void setValue(String value)
	{
		m_value = value;
	}
}
