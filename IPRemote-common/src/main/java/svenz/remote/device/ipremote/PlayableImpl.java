/**
 * 
 */
package svenz.remote.device.ipremote;

import svenz.remote.device.IPlayable;

/**
 * @author Sven Zethelius
 *
 */
public class PlayableImpl extends AbstractCoded<IPlayable.Action> implements IPlayable
{

	public PlayableImpl()
	{
		super("action", null);
	}

	@Override
	public void setAction(Action action)
	{
		fire(action.toString());
	}

	@Override
	public Action getCurrentAction()
	{
		return getStatus();
	}

}
