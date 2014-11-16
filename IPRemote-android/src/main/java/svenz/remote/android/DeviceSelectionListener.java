/**
 * 
 */
package svenz.remote.android;

import svenz.remote.device.DeviceGroupRegistry;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * @author Sven Zethelius
 *
 */
public class DeviceSelectionListener implements OnItemSelectedListener
{
	private OrderedSpinnerAdapter m_location;
	private OrderedSpinnerAdapter m_activity;
	private DeviceGroupRegistry m_registry;

	public void setActivity(OrderedSpinnerAdapter activity)
	{
		m_activity = activity;
		activity.registerListener(this);
	}

	public void setLocation(OrderedSpinnerAdapter location)
	{
		m_location = location;
		location.registerListener(this);
	}

	public void setRegistry(DeviceGroupRegistry registry)
	{
		m_registry = registry;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		// TODO determine which update it is, and see if we need to select "none"
		m_registry.setActiveDeviceGroup(m_location.getBaseSelectedItem(), m_activity.getBaseSelectedItem());
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{
		// do nothing
	}

}
