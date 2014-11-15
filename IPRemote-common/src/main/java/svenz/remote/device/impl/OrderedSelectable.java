/**
 * 
 */
package svenz.remote.device.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import svenz.remote.device.ISelectable;

/**
 * @author Sven Zethelius
 *
 */
public class OrderedSelectable implements ISelectable
{
	private final ISelectable m_selectable;
	private final List<String> m_order;

	public OrderedSelectable(ISelectable selectable, List<String> order)
	{
		m_selectable = selectable;
		m_order = order;
		m_selectable.addChangeListener(new Listener());
	}

	@Override
	public void addChangeListener(IChangeListener listener)
	{
		m_selectable.addChangeListener(listener);
	}

	@Override
	public void removeChangeListener(IChangeListener listener)
	{
		m_selectable.removeChangeListener(listener);
	}

	@Override
	public Collection<String> getOptions()
	{
		List<String> list = new ArrayList<String>(m_order);
		Collection<String> options = m_selectable.getOptions();
		list.retainAll(options);
		for (String option : options)
		{
			if (!list.contains(option))
			{
				list.add(option);
				m_order.add(option);
			}
		}

		return Collections.unmodifiableList(list);
	}

	@Override
	public void setSelection(String input)
	{
		m_selectable.setSelection(input);
	}

	@Override
	public String getSelection()
	{
		return m_selectable.getSelection();
	}

	private class Listener implements IChangeListener
	{
		@Override
		public void stateChanged(Object target, String property)
		{
			String selected = m_selectable.getSelection();
			if (selected != null && !m_order.isEmpty() && !selected.equals(m_order.get(0)))
			{
				m_order.remove(selected);
				m_order.add(0, selected);
			}
		}
	}
}
