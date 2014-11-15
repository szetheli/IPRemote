/**
 * 
 */
package svenz.remote.action;

import svenz.remote.device.IPlayable;
import svenz.remote.device.IPlayable.IPlayableAware;

/**
 * Trigger an {@link IPlayable.Action} when run
 * 
 * @author Sven Zethelius
 * 
 */
public class PlayableAction implements Runnable, IPlayableAware
{
	private IPlayable m_playable;
	private IPlayable.Action m_action;

	public void setAction(IPlayable.Action action)
	{
		m_action = action;
	}

	@Override
	public void setPlayable(IPlayable playable)
	{
		m_playable = playable;
	}

	@Override
	public void run()
	{
		m_playable.setAction(m_action);
	}

}
