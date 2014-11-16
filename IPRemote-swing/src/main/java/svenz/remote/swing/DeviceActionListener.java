/**
 * 
 */
package svenz.remote.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import javax.swing.JComboBox;
import svenz.remote.device.DeviceGroupRegistry;

/**
 * @author Sven Zethelius
 *
 */
public class DeviceActionListener implements ActionListener
{
	private static final String PREFIX = "Frame.selectors.";
	private static final String DEFAULT = DeviceGroupRegistry.INVALID_SELECTION;
	private DeviceGroupRegistry m_registry;
	private JComboBox<String> m_location;
	private JComboBox<String> m_activity;

	public void setRegistry(DeviceGroupRegistry registry)
	{
		m_registry = registry;
	}

	public void setLocation(JComboBox<String> location)
	{
		m_location = location;
		m_location.addActionListener(this);
	}

	public void setActivity(JComboBox<String> activity)
	{
		m_activity = activity;
		m_activity.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		String location =
				((String) m_location.getSelectedItem()).replace(PREFIX + "location.", "").replace("/name", "");
		String activity =
				((String) m_activity.getSelectedItem()).replace(PREFIX + "activity.", "").replace("/name", "");
		if (m_location == e.getSource())
		{
			Collection<String> activities = m_registry.getActivitiesForLocation(location);
			if (!DEFAULT.equals(location) && !activities.contains(activity))
			{
				activity = DEFAULT;
				m_activity.setSelectedItem(PREFIX + "activity." + DEFAULT + "/name");
			}
		}
		else if (m_activity == e.getSource())
		{
			Collection<String> locations = m_registry.getLocationsForActivity(activity);
			if (!DEFAULT.equals(activity) && !locations.contains(location))
			{
				location = DEFAULT;
				m_location.setSelectedItem(PREFIX + "location." + DEFAULT + "/name");
			}
		}
		else
			throw new IllegalArgumentException("Unable to match source");

		m_registry.setActiveDeviceGroup(location, activity);
	}
}
