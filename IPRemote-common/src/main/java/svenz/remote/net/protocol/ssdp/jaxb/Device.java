/**
 *
 * Device.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.net.protocol.ssdp.jaxb;

import java.util.ArrayList;
import java.util.List;

/**
 * SSDP Device description
 * 
 * @author Sven Zethelius
 * 
 */
// @javax.xml.bind.annotation.XmlRootElement(name = "device", namespace = Root.NAMESPACE)
@org.simpleframework.xml.Root(name = "device", strict = false)
@org.simpleframework.xml.Namespace(reference = Root.NAMESPACE)
public class Device // extends AbstractAnyHandler
{
	// <deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>
	@org.simpleframework.xml.Element(name = "deviceType", required = true)
	private String m_deviceType;
	// <friendlyName>VSX-1123</friendlyName>
	@org.simpleframework.xml.Element(name = "friendlyName", required = false)
	private String m_friendlyName;
	// <manufacturer>PIONEER CORPORATION</manufacturer>
	@org.simpleframework.xml.Element(name = "manufacture", required = false)
	private String m_manufacturer;
	// <modelName>VSX-1123/CUXESM</modelName>
	@org.simpleframework.xml.Element(name = "modelName", required = false)
	private String m_modelName;
	// <UDN>uuid:5F9EC1B3-ED59-79BB-4530-745e1c2f189a</UDN>
	@org.simpleframework.xml.Element(name = "UDN", required = true)
	private String m_udn;
	// <av:X_ipRemoteTcpPort xmlns:av="http://www.pioneerelectronics.com/xmlns/av">8102</av:X_ipRemoteTcpPort>
	private Integer m_ipPort;

	@org.simpleframework.xml.ElementList(name = "serviceList", entry = "service", required = false)
	private List<Service> m_services = new ArrayList<Service>(4);

	// @javax.xml.bind.annotation.XmlElement(name = "deviceType", namespace = Root.NAMESPACE, required = true)
	public String getDeviceType()
	{
		return m_deviceType;
	}

	public void setDeviceType(String deviceType)
	{
		m_deviceType = deviceType;
	}

	// @javax.xml.bind.annotation.XmlElement(name = "friendlyName", namespace = Root.NAMESPACE, required = true)
	public String getFriendlyName()
	{
		return m_friendlyName;
	}

	public void setFriendlyName(String friendlyName)
	{
		m_friendlyName = friendlyName;
	}

	// @javax.xml.bind.annotation.XmlElement(name = "manufacture", namespace = Root.NAMESPACE, required = true)
	public String getManufacturer()
	{
		return m_manufacturer;
	}

	public void setManufacturer(String manufacturer)
	{
		m_manufacturer = manufacturer;
	}

	// @javax.xml.bind.annotation.XmlElement(name = "modelName", namespace = Root.NAMESPACE, required = true)
	public String getModelName()
	{
		return m_modelName;
	}

	public void setModelName(String modelName)
	{
		m_modelName = modelName;
	}

	// @javax.xml.bind.annotation.XmlElement(name = "UDN", namespace = Root.NAMESPACE, required = true)
	public String getUDN()
	{
		return m_udn;
	}

	public void setUDN(String udn)
	{
		m_udn = udn;
	}

	// @javax.xml.bind.annotation.XmlElementWrapper(namespace = Root.NAMESPACE, name = "serviceList")
	// @javax.xml.bind.annotation.XmlElement(namespace = Root.NAMESPACE, name = "service")
	public List<Service> getServices()
	{
		return m_services;
	}

	public void setServices(List<Service> services)
	{
		m_services = services;
	}

	@org.simpleframework.xml.Element(name = "X_ipRemoteTcpPort", required = false)
	@org.simpleframework.xml.Namespace(reference = "http://www.pioneerelectronics.com/xmlns/av")
	public void setPioneerRemoteIP(int port)
	{
		m_ipPort = port;
	}

	@org.simpleframework.xml.Element(name = "X_ipRemoteTcpPort", required = false)
	@org.simpleframework.xml.Namespace(reference = "http://www.pioneerelectronics.com/xmlns/av")
	public int getPioneerRemoteIP()
	{
		throw new UnsupportedOperationException();
	}

	public Integer getIPPort()
	{
		return m_ipPort;
	}

}
