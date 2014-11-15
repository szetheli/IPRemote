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
public class VolumeChangeAction implements Runnable, ISoundAware
{
	private ISound m_sound;
	private int m_delta; // TODO change to sliding scale

	@Override
	public void setSound(ISound sound)
	{
		m_sound = sound;
	}

	public void setDelta(int delta)
	{
		m_delta = delta;
	}

	@Override
	public void run()
	{
		m_sound.setVolume(m_sound.getVolume() + m_delta);
	}
}
