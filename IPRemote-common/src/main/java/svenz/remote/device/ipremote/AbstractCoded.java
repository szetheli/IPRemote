/**
 * 
 */
package svenz.remote.device.ipremote;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.SimplePropertiesConfiguration;
import svenz.remote.device.IChangable.IChangeListener;
import svenz.remote.device.impl.ChangableImpl;

/**
 * @author Sven Zethelius
 *
 */
public class AbstractCoded<T>
{
	private final Logger m_logger = LoggerFactory.getLogger(getClass());
	private transient WritableByteChannel m_writer;
	private String m_name;
	private final Map<String, List<String>> m_codes = new HashMap<String, List<String>>();
	private final ChangableImpl<T> m_change;
	private final Map<String, String> m_codeToCommand = new HashMap<String, String>();

	public AbstractCoded(String property, Object target)
	{
		m_change = new ChangableImpl<T>(target != null ? target : this, property);
	}

	public void addChangeListener(IChangeListener listener)
	{
		m_change.addChangeListener(listener);
	}

	public void removeChangeListener(IChangeListener listener)
	{
		m_change.removeChangeListener(listener);
	}

	protected Logger getLogger()
	{
		return m_logger;
	}

	public void setDeviceListener(final ChannelDeviceListener listener)
	{
		listener.addChangeListener(new IChangeListener() {

			@Override
			public void stateChanged(Object target, String property)
			{
				AbstractCoded.this.setWriter(listener.getInstance());
			}
		});
	}

	public void setWriter(WritableByteChannel writer)
	{
		m_writer = writer;

		if (writer != null)
		{
			if (getCodes().containsKey("Query"))
				fire("Query");
		}
		else
		{
			setStatus(null);
		}
	}

	public void setName(String name)
	{
		m_name = name;
	}

	protected String getName()
	{
		return m_name;
	}

	public void query()
	{
		fire("Query");
	}

	public void setCodes(Configuration config)
	{
		for (Iterator<String> iter = config.getKeys(); iter.hasNext();)
		{
			String key = iter.next();
			String value = config.getString(key);
			m_codes.put(key, Arrays.asList(value.split(",")));
		}
		if (m_codes.isEmpty())
			throw new IllegalStateException("No codes loaded");
	}

	public void filterCodes(Collection<String> keys)
	{
		m_codes.keySet().retainAll(keys);
	}

	public String getCommand(String code)
	{
		if (!m_codeToCommand.isEmpty())
			return m_codeToCommand.get(code);

		for (Map.Entry<String, List<String>> entry : m_codes.entrySet())
		{
			List<String> codes = entry.getValue();
			if (codes.size() != 1)
				throw new IllegalStateException("codes are not single entries");
			if (!"".equals(codes.get(0)) && null != m_codeToCommand.put(codes.get(0), entry.getKey()))
				throw new IllegalStateException("codes are not unique");
		}
		return m_codeToCommand.get(code);
	}

	protected Map<String, List<String>> getCodes()
	{
		return m_codes;
	}
	
	public T getStatus()
	{
		return m_change.getStatus();
	}

	public T setStatus(T t)
	{
		getLogger().trace("Set status of {} to {}", m_name, t);
		return m_change.setStatus(t);
	}

	@Override
	public String toString()
	{
		return m_name + "(" + getStatus() + ")";
	}

	protected Configuration filterCodes(Configuration config, String... keys)
	{
		Map<String, Object> map = new HashMap<String, Object>(keys.length * 2);
		for (String key : keys)
		{
			Object o = config.getProperty(key);
			if (o != null)
				map.put(key, o);
		}

		SimplePropertiesConfiguration configFiltered = new SimplePropertiesConfiguration();
		configFiltered.setStore(map);
		return configFiltered;
	}

	protected void fire(String command)
	{
		List<String> codes = m_codes.get(command);
		if (codes == null)
			throw new IllegalArgumentException("Unable to handle '" + command + "'");

		fire(command, codes);
	}

	protected void fireFormatted(String command, Object... values)
	{
		List<String> list = getCodes().get(command);
		if (list == null || list.size() != 1)
			throw new IllegalArgumentException("Unable to handle '" + command + "'");

		fire(command, Arrays.asList(String.format(list.get(0), values)));
	}

	protected void fire(String command, List<String> codes)
	{
		WritableByteChannel writer = m_writer;
		if (writer == null)
		{
			m_logger.error("fire called with no writer present.  {} {} {}", m_name, command, codes);
			return;
		}
		for (String code : codes)
		{
			getLogger().trace("Writing command '{}' code '{}' to {}@{}", command, code, m_name, writer);
			try
			{
				writer.write(ByteBuffer.wrap(code.getBytes()));
			}
			catch (IOException e)
			{
				m_logger.error("Unable to write code '{}' for command '{}' to {}@{}", code, command, m_name, writer, e);
				return;
			}
			catch (RuntimeException e)
			{
				m_logger.error("Unable to write code '{}' for command '{}' to {}@{}", code, command, m_name, writer, e);
				return;
			}
		}
	}

}
