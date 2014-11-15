/**
 * 
 */
package svenz.remote.device.ipremote;

import svenz.remote.device.IChangable.IChangeListener;

/**
 * @author Sven Zethelius
 *
 */
public class FireChangeListener implements IChangeListener
{
	private final AbstractCoded<?> m_coded;
	private final String m_command;

	public FireChangeListener(AbstractCoded<?> coded, String command)
	{
		m_coded = coded;
		m_command = command;
	}

	@Override
	public void stateChanged(Object target, String property)
	{
		m_coded.fire(m_command);
	}
}
