/**
 * 
 */
package svenz.remote.device.ipremote;

import svenz.remote.device.IChangable.IChangeListener;

/**
 * @author Sven Zethelius
 *
 */
public class StatusFilterChangeListener<T> implements IChangeListener
{
	private final IChangeListener m_listener;
	private final T m_status;

	public StatusFilterChangeListener(IChangeListener listener, T status)
	{
		m_listener = listener;
		m_status = status;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void stateChanged(Object target, String property)
	{
		T status = ((AbstractCoded<T>) target).getStatus();
		if (m_status == status || (m_status != null && m_status.equals(status)))
			m_listener.stateChanged(target, property);
	}
}
