/**
 * 
 */
package svenz.remote.device.ipremote;

import svenz.remote.device.IMenu;

/**
 * @author Sven Zethelius
 *
 */
public class MenuImpl extends AbstractCoded<Void> implements IMenu
{

	public MenuImpl()
	{
		super(null, null);
	}

	@Override
	public void action(Action action)
	{
		fire(action.toString());
	}
}
