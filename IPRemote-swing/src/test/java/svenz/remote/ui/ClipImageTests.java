/**
 * 
 */
package svenz.remote.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import svenz.remote.swing.ClipImage;
import svenz.remote.swing.ClipImage.Direction;

/**
 * @author Sven Zethelius
 *
 */
public class ClipImageTests extends AbstractUITest
{

	@Override
	protected Component getBody()
	{
		final BufferedImage p0r = getImage("power0r.png");
		final ClipImage image = new ClipImage(p0r, Direction.Horizontal);
		image.setLevel(0);

		ImageIcon icon = new ImageIcon(image);
		final JButton button = new JButton(icon);
		final Random rand = new Random();
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				image.setLevel(rand.nextInt(10000));
				button.repaint();
			}
		});

		return button;
	}

	public static void main(String[] args) throws IOException
	{
		new ClipImageTests().show();
	}
}
