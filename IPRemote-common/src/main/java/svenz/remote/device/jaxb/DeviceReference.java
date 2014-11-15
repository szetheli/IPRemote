/**
 * 
 */
package svenz.remote.device.jaxb;


/**
 * @author Sven Zethelius
 *
 */
// @javax.xml.bind.annotation.XmlType
// @javax.xml.bind.annotation.XmlSeeAlso(MenuReference.class)
@org.simpleframework.xml.Root(strict = true)
public class DeviceReference
{
	@org.simpleframework.xml.Attribute(name = "ref", required = true)
	private String m_reference;

	public String getReference()
	{
		return m_reference;
	}

	// @javax.xml.bind.annotation.XmlAttribute(name = "ref")
	public void setReference(String reference)
	{
		m_reference = reference;
	}
}
