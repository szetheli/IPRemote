/**
 * 
 */
package svenz.remote.device.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.device.IMenu;
import svenz.remote.device.IMenu.IMenuAware;
import svenz.remote.device.IMenu.IMenusAware;
import svenz.remote.device.ISelectable;

/**
 * @author Sven Zethelius
 *
 */
public class MenuSelectable implements IMenusAware, ISelectable
{
	private static final Logger LOGGER = LoggerFactory.getLogger(MenuSelectable.class);

	private final ChangableImpl<String> m_change = new ChangableImpl<String>(this, "selection");
	private final List<String> m_menuOptions = new CopyOnWriteArrayList<String>();
	private final Map<String, IMenu> m_menus = new ConcurrentHashMap<String, IMenu>();
	private final Collection<IMenuAware> m_menuListener = new ConcurrentLinkedQueue<IMenuAware>();

	public MenuSelectable()
	{
		m_change.addChangeListener(new Listener());
	}

	@Override
	public void addChangeListener(IChangeListener listener)
	{
		m_change.addChangeListener(listener);
	}

	@Override
	public void removeChangeListener(IChangeListener listener)
	{
		m_change.removeChangeListener(listener);
	}

	public void addMenuAware(IMenuAware aware)
	{
		aware.setMenu(getMenu());
		m_menuListener.add(aware);
	}

	public void removeMenuAware(IMenuAware aware)
	{
		if (m_menuListener.remove(aware))
			aware.setMenu(null);
	}

	@Override
	public Collection<String> getOptions()
	{
		return Collections.unmodifiableCollection(m_menuOptions);
	}

	@Override
	public void setSelection(String input)
	{
		LOGGER.trace("selection set to {}", input);
		if (input != null && !m_menuOptions.contains(input))
			throw new IllegalArgumentException("Invalid menu option " + input);
		m_change.setStatus(input);
	}

	@Override
	public String getSelection()
	{
		return m_change.getStatus();
	}

	@Override
	public void addMenus(Map<String, IMenu> menus)
	{
		LOGGER.trace("Setting active menus {}", menus.keySet());
		synchronized (m_menuOptions)
		{
			m_menus.putAll(menus);
			m_menuOptions.addAll(menus.keySet());
			if (null == m_change.getStatus())
			{
				// default to first entry
				setSelection(m_menuOptions.get(0));
			}
		}
	}

	@Override
	public void removeMenus(Map<String, IMenu> menus)
	{
		LOGGER.trace("Removing active menus {}", menus.keySet());
		synchronized (m_menuOptions)
		{
			if (menus.containsKey(m_change.getStatus()))
				setSelection(null);
			m_menus.keySet().removeAll(menus.keySet());
			m_menuOptions.removeAll(menus.keySet());
		}
	}

	private IMenu getMenu()
	{
		String name = m_change.getStatus();
		return name != null ? m_menus.get(name) : null;
	}

	private void notifyMenu()
	{
		synchronized (m_menuOptions)
		{
			IMenu menu = getMenu();
			for (IMenuAware aware : m_menuListener)
				aware.setMenu(menu);
		}
	}

	private class Listener implements IChangeListener
	{
		@Override
		public void stateChanged(Object target, String property)
		{
			notifyMenu();
		}
	}
}
