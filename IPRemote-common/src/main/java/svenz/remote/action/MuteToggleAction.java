/**
 * 
 */
package svenz.remote.action;

import svenz.remote.device.ISound;
import svenz.remote.device.ISound.ISoundAware;

/**
 * @author Sven Zethelius
 *
 */
public class MuteToggleAction implements Runnable, ISoundAware
{
	private ISound m_sound;

	@Override
	public void setSound(ISound sound)
	{
		m_sound = sound;
	}

	@Override
	public void run()
	{
		m_sound.mute(!m_sound.isMute());
	}
}
