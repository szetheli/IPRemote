/**
 * 
 */
package svenz.remote.device.ipremote;

import svenz.remote.device.IChangable.IChangeListener;
import svenz.remote.device.IPowered;

/**
 * If powered, send query. If unpowered, clear status
 * 
 * @author Sven Zethelius
 * 
 */
public class QueryChangeListener implements IChangeListener
{
	private final AbstractCoded<?>[] m_coded;

	public QueryChangeListener(AbstractCoded<?>... coded)
	{
		super();
		m_coded = coded;
	}

	@Override
	public void stateChanged(Object target, String property)
	{
		if (((IPowered) target).isPowered())
		{
			for (AbstractCoded<?> coded : m_coded)
			{
				if (coded.getStatus() == null)
				{
					coded.query();
				}
			}
		}
		else
		{
			for (AbstractCoded<?> coded : m_coded)
			{
				coded.setStatus(null);
			}
		}
	}

}
