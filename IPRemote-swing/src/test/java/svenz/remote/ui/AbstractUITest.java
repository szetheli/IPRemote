/**
 * 
 */
package svenz.remote.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import svenz.remote.common.utilities.LoggingRunnable;
import svenz.remote.swing.CloseActionListener;

/**
 * @author Sven Zethelius
 *
 */
public abstract class AbstractUITest
{
	protected abstract Component getBody();

	protected BufferedImage getImage(String name)
	{
		try
		{
			return ImageIO.read(getClass().getResourceAsStream(name));
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	protected void show()
	{
		SwingUtilities.invokeLater(new LoggingRunnable(new Runnable() {

			@Override
			public void run()
			{
				JPanel panel = new JPanel(new FlowLayout());

				panel.add(getBody());
				CloseActionListener.initKeyBindings(panel);

				JFrame frame = new JFrame();
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				Container contentPane = frame.getContentPane();
				contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
				contentPane.add(panel);

				Dimension dimension = new Dimension(200, 200);
				frame.setMinimumSize(dimension);
				frame.setVisible(true);
			}
		}));

	}
}
