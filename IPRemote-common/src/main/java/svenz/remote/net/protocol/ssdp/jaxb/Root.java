/**
 *
 * Root.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.net.protocol.ssdp.jaxb;


/**
 * SSDP Root description
 * 
 * @author Sven Zethelius
 * 
 */
// @javax.xml.bind.annotation.XmlRootElement(name = "root", namespace = Root.NAMESPACE)
@org.simpleframework.xml.Root(name = "root", strict = false)
@org.simpleframework.xml.Namespace(reference = Root.NAMESPACE)
public class Root // extends AbstractAnyHandler
{
	public static final String NAMESPACE = "urn:schemas-upnp-org:device-1-0";

	// specVersion
	// - major
	// - minor
	// device

	@org.simpleframework.xml.Element(name = "device")
	private Device m_device;

	// @javax.xml.bind.annotation.XmlElement(name = "device", namespace = Root.NAMESPACE, required = true)
	public Device getDevice()
	{
		return m_device;
	}

	public void setDevice(Device device)
	{
		m_device = device;
	}

}
