/**
 * 
 */
package svenz.remote.swing;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import svenz.remote.ui.IClipImage;

/**
 * @author Sven Zethelius
 *
 */
public class ClipImage extends BufferedImage implements IClipImage
{
	public static enum Direction
	{
		Horizontal, Vertical,
	}

	private final Image m_imageSrc;
	private final Dimension m_dimension;
	private final Direction m_direction;
	
	private static Dimension getDimensions(Image image)
	{
		return new Dimension(image.getWidth(null), image.getHeight(null));
	}

	public ClipImage(Image imageSrc, Direction direction)
	{
		this(getDimensions(imageSrc), imageSrc, direction);
	}

	private ClipImage(Dimension dimension, Image imageSrc, Direction direction)
	{
		super(dimension.width, dimension.height, TYPE_INT_ARGB);
		m_direction = direction;
		m_dimension = dimension;
		m_imageSrc = imageSrc;
	}

	@Override
	public void setLevel(int clipSize)
	{
		Graphics2D g = (Graphics2D) getGraphics();
		int w = m_dimension.width, h = m_dimension.height;
		if (m_direction == Direction.Horizontal)
			w = clipSize * w * 10 / 100000;
		else
			h = clipSize * h * 10 / 100000;

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
		g.fillRect(0, 0, m_dimension.width, m_dimension.height);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
		g.setClip(0, 0, w, h);
		g.drawImage(m_imageSrc, 0, 0, null);

	}
}
