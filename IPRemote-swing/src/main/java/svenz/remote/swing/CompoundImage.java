/**
 * 
 */
package svenz.remote.swing;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Sven Zethelius
 *
 */
public class CompoundImage extends BufferedImage
{
	private final List<Image> m_images;
	private final Dimension m_dimension;

	private static Dimension getDimensions(Collection<Image> images)
	{
		int w = 0;
		int h = 0;
		for(Image image : images)
		{

			w = Math.max(image.getWidth(null), w);
			h = Math.max(image.getHeight(null), h);
		}
		return new Dimension(w, h);
	}

	public CompoundImage(Image... images)
	{
		this(Arrays.asList(images));
	}

	public CompoundImage(List<Image> images)
	{
		this(getDimensions(images), images);
	}

	private CompoundImage(Dimension dimension, List<Image> images)
	{
		super(dimension.width, dimension.height, TYPE_INT_ARGB);
		m_images = new ArrayList<Image>(images);
		m_dimension = dimension;
		update(null);
	}

	public void setImage(int index, Image image, ImageObserver observer)
	{
		m_images.set(index, image);
		update(observer);
	}

	private void update(ImageObserver observer)
	{
		Graphics2D g = (Graphics2D) getGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
		g.fillRect(0, 0, m_dimension.width, m_dimension.height);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		for (Iterator<Image> iter = m_images.iterator(); iter.hasNext();)
		{
			Image image = iter.next();
			g.drawImage(image, 0, 0, iter.hasNext() ? null : observer);
		}
	}
}
