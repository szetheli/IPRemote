/**
 * 
 */
package svenz.remote.common.utilities;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.SubsetConfiguration;

/**
 * Override of {@link SubsetConfiguration} that removes the interpolate feature to improve read speed.
 * 
 * @author Sven Zethelius
 * 
 */
public class SimpleSubsetConfiguration extends SubsetConfiguration
{
	public SimpleSubsetConfiguration(Configuration parent, String prefix, String delimiter)
	{
		super(parent, prefix, delimiter);
	}

	@Override
	protected Object interpolate(Object base)
	{
		return base;
	}

	@Override
	public Configuration subset(String prefix)
	{
		return new SimpleSubsetConfiguration(parent, prefix, delimiter);
	}
}
