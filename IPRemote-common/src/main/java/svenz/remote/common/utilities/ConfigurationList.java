/**
 * 
 */
package svenz.remote.common.utilities;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration.Configuration;

/**
 * @author Sven Zethelius
 *
 */
public class ConfigurationList<T> extends AbstractList<T>
{
	private final Configuration m_config;
	private final String m_key;
	private final List<T> m_list;

	@SuppressWarnings("unchecked")
	public ConfigurationList(Configuration config, String key)
	{
		super();
		m_config = config;
		m_key = key;
		List<T> list = (List<T>) config.getList(key);
		m_list = list == null ? new ArrayList<T>() : list;
	}

	@Override
	public T get(int index)
	{
		return m_list.get(index);
	}

	@Override
	public int size()
	{
		return m_list.size();
	}

	@Override
	public void add(int index, T element)
	{
		int idxOld = m_list.indexOf(element);
		if (idxOld > -1)
			m_list.remove(idxOld);
		m_list.add(index, element);
		m_config.setProperty(m_key, m_list);
	}

	@Override
	public boolean remove(Object o)
	{
		boolean remove = m_list.remove(o);
		if (remove)
			m_config.setProperty(m_key, m_list);
		return remove;
	}

	@Override
	public T remove(int index)
	{
		T remove = m_list.remove(index);
		m_config.setProperty(m_key, m_list);
		return remove;
	}
}
