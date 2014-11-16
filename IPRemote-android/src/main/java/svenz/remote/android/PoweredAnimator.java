/**
 * 
 */
package svenz.remote.android;

import svenz.remote.ui.IClipImage;
import svenz.remote.ui.PowerAnimator;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

/**
 * Layering: Off, power*On, power*Off
 * 
 * @author Sven Zethelius
 * 
 */
public class PoweredAnimator implements OnClickListener
{
	private final PowerAnimator m_animator = new PowerAnimator();

	public PoweredAnimator(ImageButton powerButton)
	{
		if (powerButton.getHandler() == null)
			throw new IllegalStateException("No handler for powerButton");
		m_animator.setExecutor(new HandlerExecutor(powerButton.getHandler()));

		LayerDrawable layer = (LayerDrawable) powerButton.getDrawable();
		m_animator.setStickOn(adapt(layer, R.id.power_stick_on));
		m_animator.setStickOff(adapt(layer, R.id.power_stick_off));
		m_animator.setRingOn(adapt(layer, R.id.power_ring_on));
		m_animator.setRingOff(adapt(layer, R.id.power_ring_off));
	}

	private static IClipImage adapt(LayerDrawable layer, int id)
	{
		return new ClipDrawableAdapter((ClipDrawable) layer.findDrawableByLayerId(id));
	}

	public PowerAnimator getAnimator()
	{
		return m_animator;
	}


	@Override
	public void onClick(View v)
	{
		m_animator.toggle();
	}
}
