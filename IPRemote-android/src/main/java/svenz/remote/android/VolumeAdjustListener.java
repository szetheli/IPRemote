/**
 * 
 */
package svenz.remote.android;

import svenz.remote.device.IChangable.IChangeListener;
import svenz.remote.device.ISound;
import svenz.remote.device.ISound.ISoundAware;
import android.content.Context;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.widget.ImageButton;

/**
 * @author Sven Zethelius
 *
 */
public class VolumeAdjustListener implements IChangeListener, ISoundAware
{
	private final Runnable m_runnable = new UpdateRunnable();
	private ISound m_sound;
	private final ClipDrawable m_volumeClip;
	private final ClipDrawable m_muteClip;
	private final Handler m_handler;
	private final int m_volumeLevels;
	private final int m_volumeLevelStep;

	public VolumeAdjustListener(ImageButton button)
	{
		m_handler = button.getHandler();
		LayerDrawable drawable = (LayerDrawable) button.getDrawable();
		m_volumeClip = (ClipDrawable) drawable.findDrawableByLayerId(R.id.mute_base_clip);
		m_muteClip = (ClipDrawable) drawable.findDrawableByLayerId(R.id.mute_on_clip);

		Context context = button.getContext();
		m_volumeLevels = Integer.parseInt(context.getString(R.string.volume_level_steps));
		m_volumeLevelStep = Integer.parseInt(context.getString(R.string.volume_level_step));
		update();
	}

	// TODO Test

	@Override
	public void stateChanged(Object target, String property)
	{
		m_handler.post(m_runnable);
	}

	@Override
	public void setSound(ISound sound)
	{
		synchronized (this)
		{
			if (m_sound != null)
				m_sound.removeChangeListener(this);
			m_sound = sound;
			if (m_sound != null)
				m_sound.addChangeListener(this);
		}
		m_handler.post(m_runnable);
	}

	private void update()
	{
		boolean mute;
		int level;
		
		synchronized (this)
		{
			if (m_sound == null)
			{
				mute = false;
				level = 0;
			}
			else
			{
				mute = m_sound.isMute();
				level =
						m_sound.getVolume() * m_volumeLevels
								/ (m_sound.getVolumeMaximum() - m_sound.getVolumeMinimum());
			}
			
			m_muteClip.setLevel(mute ? 10000 : 0);
			m_volumeClip.setLevel(10000 - ((m_volumeLevels - level) * m_volumeLevelStep));

		}
	}

	private class UpdateRunnable implements Runnable
	{
		@Override
		public void run()
		{
			update();
		}
	}
}
