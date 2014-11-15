/**
 * 
 */
package svenz.remote.action;

import svenz.remote.device.IMenu;
import svenz.remote.device.IMenu.IMenuAware;

/**
 * Trigger {@link IMenu.Action} when run
 * 
 * @author Sven Zethelius
 * 
 */
public class MenuAction implements Runnable, IMenuAware
{
	private IMenu m_menu;
	private IMenu.Action m_action;

	public void setAction(IMenu.Action action)
	{
		m_action = action;
	}

	@Override
	public void setMenu(IMenu menu)
	{
		m_menu = menu;
	}

	@Override
	public void run()
	{
		if (m_menu == null)
			throw new IllegalStateException("No current menu set to execute action " + m_action);
		m_menu.action(m_action);
	}

}
