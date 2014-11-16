/**
 * 
 */
package svenz.remote.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Sven Zethelius
 *
 */
public class ActionMouseListener extends MouseAdapter
{
	private final ActionListener m_listener;

	public ActionMouseListener(ActionListener listener)
	{
		super();
		m_listener = listener;
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		m_listener.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, null));
	}
}
