/**
 *
 * CloseActionListener.java
 *
 * Copyright 2013 Sven Zethelius. All rights reserved.
 */
package svenz.remote.swing;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Closes the window when action is triggered.
 * 
 * @author Sven Zethelius
 * 
 */
public class CloseActionListener implements ActionListener
{
	private static final ActionListener INSTANCE = new CloseActionListener();

	@Override
	public void actionPerformed(ActionEvent e)
	{
		JComponent c = (JComponent) e.getSource();
		((Window) SwingUtilities.getRoot(c)).dispose();
	}

	public static void initKeyBindings(JComponent component)
	{
		component.registerKeyboardAction(INSTANCE, KeyStroke.getKeyStroke("control W"),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		component.registerKeyboardAction(INSTANCE, KeyStroke.getKeyStroke("meta W"), 
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		component.registerKeyboardAction(INSTANCE, KeyStroke.getKeyStroke("alt F4"),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
	}
}
