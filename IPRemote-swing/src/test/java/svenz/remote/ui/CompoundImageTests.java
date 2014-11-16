/**
 * 
 */
package svenz.remote.ui;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import svenz.remote.swing.CompoundImage;

/**
 * @author Sven Zethelius
 *
 */
public class CompoundImageTests extends AbstractUITest
{
	@Override
	protected Component getBody()
	{
		final BufferedImage p0r = getImage("power0r.png"), p0s = getImage("power0s.png");
		final CompoundImage image = new CompoundImage(p0r, p0s);

		ImageIcon icon = new ImageIcon(image);
		final JButton button = new JButton(icon);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e)
			{
				Graphics2D g = (Graphics2D) p0s.getGraphics();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
				g.fillRect(0, 0, p0s.getWidth(), p0s.getHeight());

				image.setImage(1, p0s, button);
			}
		});

		return button;
	}

	public static void main(String[] args) throws IOException
	{
		new CompoundImageTests().show();
	}
}
