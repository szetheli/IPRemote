/**
 * 
 */
package svenz.remote.action;

import svenz.remote.device.ISelectable;

/**
 * Calls {@link ISelectable#setSelection(String)} to a specific value when run.
 * 
 * @author Sven Zethelius
 * 
 */
public class SetSelectionAction implements Runnable
{
	private String m_option;
	private ISelectable m_selectable;

	public void setSelection(String option)
	{
		m_option = option;
	}

	public void setSelectable(ISelectable selectable)
	{
		m_selectable = selectable;
	}

	@Override
	public void run()
	{
		m_selectable.setSelection(m_option);
	}

}
