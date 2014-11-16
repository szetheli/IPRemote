/**
 * 
 */
package svenz.remote.swing;

import java.awt.Component;
import java.util.Collection;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.apache.commons.collections.Transformer;
import svenz.remote.action.SetSelectionAction;
import svenz.remote.common.utilities.LoggingRunnable;
import svenz.remote.device.ISelectable;

/**
 * When run, pops up a menu of the selectable options
 * 
 * @author Sven Zethelius
 * 
 */
public class SelectablePopupRunnable implements Runnable
{
	private ISelectable m_selectable;
	private Transformer m_transformer;
	private Component m_target;

	public void setTransformer(Transformer transformer)
	{
		m_transformer = transformer;
	}

	public void setSelectable(ISelectable selectable)
	{
		m_selectable = selectable;
	}

	public void setTarget(Component target)
	{
		m_target = target;
	}

	@Override
	public void run()
	{
		JPopupMenu pMenu = new JPopupMenu();
		Collection<String> options = m_selectable.getOptions();
		if (options.isEmpty())
			throw new IllegalStateException("No options to select in current state");
		for (String option : options)
		{
			String name = (String) m_transformer.transform(option);
			JMenuItem item = new JMenuItem(name);
			item.setRolloverEnabled(true);
			SetSelectionAction action = new SetSelectionAction();
			action.setSelectable(m_selectable);
			action.setSelection(option);
			item.addActionListener(new RunnableActionListener(new LoggingRunnable(action)));
			pMenu.add(item);
		}
		pMenu.show(m_target, 0, 0);
	}
}
