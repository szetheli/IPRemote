/**
 * 
 */
package svenz.remote.android;

import svenz.remote.ui.IClipImage;
import android.graphics.drawable.ClipDrawable;

/**
 * @author Sven Zethelius
 *
 */
public class ClipDrawableAdapter implements IClipImage
{
	private final ClipDrawable m_drawable;

	public ClipDrawableAdapter(ClipDrawable drawable)
	{
		m_drawable = drawable;
	}

	@Override
	public void setLevel(int clipSize)
	{
		m_drawable.setLevel(clipSize);
	}

}
