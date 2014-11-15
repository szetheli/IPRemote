/**
 * 
 */
package svenz.remote.common.utilities;

import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.configuration.AbstractFileConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Replacement for {@link PropertiesConfiguration} that dramatically increases read speed at the expense of
 * interpolation and data type conversion features.
 * 
 * @author Sven Zethelius
 * 
 */
public class SimplePropertiesConfiguration extends AbstractFileConfiguration implements Configuration
{
	private Map<String, Object> m_store;

	@Override
	protected Object interpolate(Object value)
	{
		return value;
	}

	public Map<String, Object> getStore()
	{
		return m_store;
	}

	public void setStore(Map<String, Object> store)
	{
		m_store = store;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void load(Reader in) throws ConfigurationException
	{
		Properties props = new Properties();
		try
		{
			props.load(in);
			m_store = (Map) props;
		}
		catch (Exception e)
		{
			throw new ConfigurationException(e);
		}
		finally
		{
			Utilities.safeClose(in);
		}
	}

	@Override
	public void save(Writer out) throws ConfigurationException
	{
		throw new UnsupportedOperationException("Save");
	}

	@Override
	public Object getProperty(String key)
	{
		return m_store.get(key);
	}

	@Override
	protected void addPropertyDirect(String key, Object value)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty()
	{
		return m_store.isEmpty();
	}

	@Override
	public boolean containsKey(String key)
	{
		return m_store.containsKey(key);
	}

	@Override
	protected void clearPropertyDirect(String key)
	{
		m_store.remove(key);
	}

	@Override
	public Iterator<String> getKeys()
	{
		return m_store.keySet().iterator();
	}
}
