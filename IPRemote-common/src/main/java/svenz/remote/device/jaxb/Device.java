/**
 * 
 */
package svenz.remote.device.jaxb;


/**
 * @author Sven Zethelius
 *
 */
// @javax.xml.bind.annotation.XmlRootElement(name = "Device")
@org.simpleframework.xml.Root(name = "Device", strict = true)
public class Device
{
	@org.simpleframework.xml.Attribute(name = "name", required = true)
	private String m_name;
	@org.simpleframework.xml.Attribute(name = "impl", required = true)
	private Class<?> m_clazz;

	// @javax.xml.bind.annotation.XmlAttribute(name = "name")
	public void setName(String name)
	{
		m_name = name;
	}

	public String getName()
	{
		return m_name;
	}

	// @javax.xml.bind.annotation.XmlAttribute(name = "impl")
	public void setImplClass(String className) throws ClassNotFoundException
	{
		m_clazz = Class.forName(className);
	}

	public Object getImplInstance() throws InstantiationException, IllegalAccessException
	{
		return m_clazz.newInstance();
	}
}
