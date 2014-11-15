/**
 * 
 */
package svenz.remote.device.ipremote;

import org.apache.commons.configuration.Configuration;
import svenz.remote.device.IPowered;

/**
 * @author Sven Zethelius
 *
 */
public class PoweredImpl extends AbstractCoded<Boolean> implements IPowered
{
	public PoweredImpl()
	{
		super("powered", null);
	}

	@Override
	public boolean isPowered()
	{
		// null = false
		return Boolean.TRUE.equals(getStatus());
	}

	@Override
	public void powerOn()
	{
		fire("On");
	}

	@Override
	public void powerOff()
	{
		fire("Off");
	}

	@Override
	public void setCodes(Configuration config)
	{
		super.setCodes(filterCodes(config, "On", "Off", "Query"));
	}

	// TODO how to handle chained response
}
