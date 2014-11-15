/**
 *
 * Service.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.net.protocol.ssdp.jaxb;


/**
 * SSDP Service description
 * 
 * @author Sven Zethelius
 * 
 */
// @javax.xml.bind.annotation.XmlRootElement(namespace = Root.NAMESPACE, name = "service")
@org.simpleframework.xml.Root(name = "service", strict = false)
public class Service // extends AbstractAnyHandler
{
	private String m_serviceType;
	private String m_serviceId;
	private String m_scpdURL;
	private String m_controlURL;
	private String m_eventSubURL;

	// @javax.xml.bind.annotation.XmlElement(namespace = Root.NAMESPACE, name = "serviceType")
	public String getServiceType()
	{
		return m_serviceType;
	}

	public void setServiceType(String serviceType)
	{
		m_serviceType = serviceType;
	}

	// @javax.xml.bind.annotation.XmlElement(namespace = Root.NAMESPACE, name = "serviceId")
	public String getServiceId()
	{
		return m_serviceId;
	}

	public void setServiceId(String serviceId)
	{
		m_serviceId = serviceId;
	}

	// @javax.xml.bind.annotation.XmlElement(namespace = Root.NAMESPACE, name = "SCPDURL")
	public String getSCPDURL()
	{
		return m_scpdURL;
	}

	public void setSCPDURL(String scpdURL)
	{
		m_scpdURL = scpdURL;
	}

	// @javax.xml.bind.annotation.XmlElement(namespace = Root.NAMESPACE, name = "controlURL")
	public String getControlURL()
	{
		return m_controlURL;
	}

	public void setControlURL(String controlURL)
	{
		m_controlURL = controlURL;
	}

	// @javax.xml.bind.annotation.XmlElement(namespace = Root.NAMESPACE, name = "eventSubURL")
	public String getEventSubURL()
	{
		return m_eventSubURL;
	}

	public void setEventSubURL(String eventSubURL)
	{
		m_eventSubURL = eventSubURL;
	}

}
