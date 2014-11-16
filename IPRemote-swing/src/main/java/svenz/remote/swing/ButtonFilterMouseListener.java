/**
 * 
 */
package svenz.remote.swing;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author Sven Zethelius
 *
 */
public class ButtonFilterMouseListener extends MouseAdapter
{
	private final MouseListener m_listener;
	private final int m_button;

	public ButtonFilterMouseListener(MouseListener listener, int button)
	{
		m_listener = listener;
		m_button = button;
	}

	private boolean matchs(MouseEvent e)
	{
		return e.getButton() == m_button;
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (matchs(e))
			m_listener.mouseClicked(e);
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (matchs(e))
			m_listener.mousePressed(e);
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (matchs(e))
			m_listener.mouseReleased(e);
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		m_listener.mouseEntered(e);
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		m_listener.mouseExited(e);
	}

}
