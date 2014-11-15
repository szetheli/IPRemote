/**
 *
 * AbstractAnyHandler.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.net.protocol.ssdp.jaxb;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.commons.collections.map.MultiValueMap;
import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.strategy.Visitor;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NodeMap;
import org.simpleframework.xml.stream.OutputNode;
import org.w3c.dom.Element;

/**
 * JAXB object that supports XSD ANY, intended to be extended by JAXB classes
 * 
 * @author Sven Zethelius
 * 
 */
public class AbstractAnyHandler
{
	private final AnyAdapterList m_any = new AnyAdapterList();

	// @javax.xml.bind.annotation.XmlAnyElement(lax = false)
	public Collection<Element> getAny()
	{
		return m_any;
	}
	
	/**
	 * Get an element that matches name
	 * 
	 * @param name
	 * @return the first matched element, or null if the element doesn't exist.
	 * @throws IllegalStateException
	 *             if multiple elements are found for the same name
	 */
	public Element get(QName name)
	{
		List<Element> list = m_any.getNodes().get(name);
		if (list == null)
			return null;
		if (list.size() == 1)
			return list.get(0);
		throw new IllegalStateException("Multiple elements found for " + name);
	}

	/**
	 * Get the text of an element that matches name. Element must be type xsd:string
	 * 
	 * @param name
	 * @return the text
	 * @throws IllegalStateException
	 *             if multiple elements are found for the same name
	 */
	public String getValue(QName name)
	{
		Element e = get(name);
		return e == null || e.getChildNodes().getLength() != 1 ? null : e.getChildNodes().item(0).getNodeValue();
	}

	/**
	 * Adapter to maintain a "List" of Elements that can be quickly accessed via hash map. Does not maintain strict
	 * element order.
	 * 
	 * @author Sven Zethelius
	 * 
	 */
	@SuppressWarnings("unchecked")
	private class AnyAdapterList extends AbstractCollection<Element>
	{
		private final MultiValueMap/* <QName, Element> */m_nodes =
				MultiValueMap.decorate(new LinkedHashMap<QName, Object>());

		public Map<QName, List<Element>> getNodes()
		{
			return m_nodes;
		}

		@Override
		public boolean add(Element e)
		{
			QName name = new QName(e.getNamespaceURI(), e.getLocalName());
			return m_nodes.put(name, e) != null;
		}

		@Override
		public Iterator<Element> iterator()
		{
			return m_nodes.values().iterator();
		}

		@Override
		public int size()
		{
			return m_nodes.totalSize();
		}

	}

	public static class AnyAdapterVisitor implements Visitor
	{

		@Override
		public void read(Type type, NodeMap<InputNode> node) throws Exception
		{


		}

		@Override
		public void write(Type type, NodeMap<OutputNode> node) throws Exception
		{
			throw new UnsupportedOperationException("Write");
		}

	}
}
