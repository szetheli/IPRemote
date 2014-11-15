/**
 * 
 */
package svenz.remote.device.ipremote;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import org.apache.commons.configuration.Configuration;
import svenz.remote.device.ISelectable;

/**
 * @author Sven Zethelius
 *
 */
public class SelectableImpl extends AbstractCoded<String> implements ISelectable
{
	private Collection<String> m_options = Collections.emptyList();
	private boolean m_formatted;

	public SelectableImpl()
	{
		super("selection", null);
	}

	@Override
	public Collection<String> getOptions()
	{
		return m_options;
	}

	public void setOptions(Collection<String> options)
	{
		m_options = Collections.unmodifiableCollection(new LinkedHashSet<String>(options));
	}

	@Override
	public void setCodes(Configuration config)
	{
		super.setCodes(config);
		m_formatted = getCodes().get("Set") != null;
		if (!m_formatted)
			setOptions(getCodes().keySet());
	}

	@Override
	public void setSelection(String input)
	{
		if (!m_options.contains(input))
			throw new IllegalArgumentException("Unable to set status to '" + input + "'");

		if (m_formatted)
			fireFormatted("Set", input);
		else
			fire(input);
	}

	@Override
	public String setStatus(String input)
	{
		if (input == null || m_options.contains(input))
			return super.setStatus(input);
		else
			throw new IllegalArgumentException("Unable to set status to '" + input + "'");
	}

	@Override
	public String getSelection()
	{
		return getStatus();
	}

}
