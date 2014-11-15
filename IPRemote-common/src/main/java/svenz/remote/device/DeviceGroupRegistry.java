/**
 * 
 */
package svenz.remote.device;

import java.beans.IntrospectionException;
import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.common.utilities.BeanAdapter;
import svenz.remote.common.utilities.Utilities;
import svenz.remote.device.IMenu.IMenusAware;
import svenz.remote.device.IPlayable.IPlayableAware;
import svenz.remote.device.IPowered.IPoweredAware;
import svenz.remote.device.ISound.ISoundAware;
import svenz.remote.device.impl.ChangableImpl;
import svenz.remote.device.ipremote.AbstractDevice;
import svenz.remote.device.jaxb.DeviceGroups;
import svenz.remote.device.jaxb.DeviceReference;
import svenz.remote.device.jaxb.Group;
import svenz.remote.device.jaxb.MenuReference;
import svenz.remote.device.jaxb.SelectableReference;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.protocol.ssdp.SSDPManager;

/**
 * Manages a set of devices
 * 
 * @author Sven Zethelius
 * 
 */
public class DeviceGroupRegistry implements Closeable, IChangable
{
	public static final String INVALID_SELECTION = "none";
	private static final Logger LOGGER = LoggerFactory.getLogger(DeviceGroupRegistry.class);
	private static final DeviceGroup NULL = new DeviceGroup();
	private final Map<List<String>, DeviceGroup> m_deviceGroups = new HashMap<List<String>, DeviceGroup>();
	private final List<DeviceVisitor<?>> m_registered = 
			new ArrayList<DeviceVisitor<?>>(Arrays.asList(new SelectableDeviceVisitor()));
	private final Collection<AbstractDevice> m_devices = new ArrayList<AbstractDevice>(4);


	private ScheduledExecutorService m_executor;
	private SocketChannelManager m_channelManager;
	private SSDPManager m_ssdpManager;
	private final ChangableImpl<DeviceGroup> m_change = new ChangableImpl<DeviceGroup>(this, "activeDeviceGroup");

	public DeviceGroupRegistry()
	{
		init();
	}

	public void setExecutor(ScheduledExecutorService executor)
	{
		synchronized (m_devices)
		{
			for (AbstractDevice device : m_devices)
				device.setExecutor(executor);
		}

		m_executor = executor;
	}

	public void setChannelManager(SocketChannelManager channelManager)
	{
		synchronized (m_devices)
		{
			for (AbstractDevice device : m_devices)
				device.setChannelManager(channelManager);
		}
		m_channelManager = channelManager;
	}

	public void setSSDPManager(SSDPManager ssdpManager)
	{
		synchronized (m_devices)
		{
			for (AbstractDevice device : m_devices)
				device.setSSDPManager(ssdpManager);
		}
		m_ssdpManager = ssdpManager;
	}

	public void open()
	{
		synchronized (m_devices)
		{
			for (AbstractDevice device : m_devices)
				device.open();
		}
	}

	@Override
	public void close()
	{
		synchronized (m_devices)
		{
			for (AbstractDevice device : m_devices)
				Utilities.safeClose(device);
			m_devices.clear();
		}
		m_deviceGroups.clear();
		init();
	}

	private void init()
	{
		m_deviceGroups.put(Arrays.asList(INVALID_SELECTION, INVALID_SELECTION), NULL);
		m_change.setStatus(NULL);
	}
	
	@Override
	public void addChangeListener(IChangeListener listener)
	{
		m_change.addChangeListener(listener);
	}

	@Override
	public void removeChangeListener(IChangeListener listener)
	{
		m_change.removeChangeListener(listener);
	}

	/**
	 * Load the devices described by the {@link DeviceGroups}
	 * 
	 * @param deviceGroups
	 */
	public void load(DeviceGroups deviceGroups)
	{
		try
		{
			BeanAdapter bean = new BeanAdapter();
			for (svenz.remote.device.jaxb.Device device : deviceGroups.getDevices())
				bean.addRoot(device.getName(), initDevice(device.getImplInstance()));

			for (Group group : deviceGroups.getGroups())
			{
				DeviceGroup dgroup = new DeviceGroup();
				dgroup.setPlayable((IPlayable) resolve(dgroup, bean, group.getPlayable()));
				dgroup.setSound((ISound) resolve(dgroup, bean, group.getSound()));
				for (MenuReference ref : group.getMenuReferences())
					dgroup.addMenu((IMenu) resolve(dgroup, bean, ref), ref.getName());
				for (SelectableReference ref : group.getSelectableReferences())
					resolve(dgroup, bean, ref);

				m_deviceGroups.put(Arrays.asList(group.getLocation(), group.getActivity()), dgroup);
			}
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	private AbstractDevice initDevice(Object o)
	{
		AbstractDevice device = (AbstractDevice) o;
		device.setChannelManager(m_channelManager);
		device.setExecutor(m_executor);
		device.setSSDPManager(m_ssdpManager);
		m_devices.add(device);
		return device;
	}

	private Object resolve(DeviceGroup group, BeanAdapter adapter, DeviceReference ref) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IntrospectionException, NoSuchMethodException,
			SecurityException
	{
		if (ref == null)
			return null;
		List<String> path = Arrays.asList(ref.getReference().split("\\."));
		Object o = adapter.resolve(path);
		resolvePower(group, adapter, path);
		return o;
	}

	private void resolve(DeviceGroup group, BeanAdapter adapter, SelectableReference ref)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException,
			NoSuchMethodException, SecurityException
	{
		List<String> path = Arrays.asList(ref.getReference().split("\\."));
		ISelectable selectable = (ISelectable) adapter.resolve(path);
		IPowered powered = resolvePower(group, adapter, path);
		group.addSelectable(selectable, powered, ref.getValue());
	}

	private IPowered resolvePower(DeviceGroup group, BeanAdapter adapter, List<String> path)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException,
			NoSuchMethodException, SecurityException
	{
		path = new ArrayList<String>(path);
		path.set(path.size() - 1, "powered");
		IPowered powered = (IPowered) adapter.resolve(path);
		group.addPowered(powered);
		return powered;
	}

	/**
	 * Set the active set of devices
	 * 
	 * @param location
	 * @param activity
	 */
	public void setActiveDeviceGroup(String location, String activity)
	{
		LOGGER.trace("Setting active device group to {}@{}", activity, location);
		synchronized (this)
		{
			DeviceGroup deviceGroup = m_deviceGroups.get(Arrays.asList(location, activity));
			if (deviceGroup == null)
				deviceGroup = NULL;
			DeviceGroup deviceGroupOld = m_change.setStatus(deviceGroup);
			if (deviceGroupOld != deviceGroup)
			{
				for (DeviceVisitor<?> v : m_registered)
					v.update(deviceGroup, deviceGroupOld);
			}

		}
	}

	/**
	 * Get the set of all locations
	 * 
	 * @return
	 */
	public Collection<String> getLocations()
	{
		Collection<String> list = new LinkedHashSet<String>();
		for (List<String> keys : m_deviceGroups.keySet())
			list.add(keys.get(0));
		return list;
	}

	/**
	 * Get the locations that support activity
	 * 
	 * @param activity
	 * @return
	 */
	public Collection<String> getLocationsForActivity(String activity)
	{
		Collection<String> list = new LinkedHashSet<String>();
		for (List<String> keys : m_deviceGroups.keySet())
			if (keys.get(1).equals(activity))
				list.add(keys.get(0));
		return list;
	}

	/**
	 * Get the set of all activities
	 * 
	 * @return
	 */
	public Collection<String> getActivities()
	{
		Collection<String> list = new LinkedHashSet<String>();
		for (List<String> keys : m_deviceGroups.keySet())
			list.add(keys.get(1));
		return list;
	}

	/**
	 * Get the valid activities for the location
	 * 
	 * @param location
	 * @return
	 */
	public Collection<String> getActivitiesForLocation(String location)
	{
		Collection<String> list = new LinkedHashSet<String>();
		for (List<String> keys : m_deviceGroups.keySet())
			if (keys.get(0).equals(location))
				list.add(keys.get(1));
		return list;
	}
	
	/**
	 * @return the activeDeviceGroup
	 */
	public IDeviceGroup getActiveDeviceGroup()
	{
		DeviceGroup d = m_change.getStatus();
		return d == NULL ? null : d;
	}

	private <T> void register(T t, DeviceVisitor<T> device)
	{
		device.setObject(t);
		m_registered.add(device);
		DeviceGroup d = m_change.getStatus();
		if (d != NULL)
			device.update(d, NULL);
	}
	
	public void register(IPoweredAware a)
	{
		register(a, new PoweredDeviceVisitor());
	}

	public void register(IPlayableAware a)
	{
		register(a, new PlayableDeviceVisitor());
	}

	public void register(ISoundAware a)
	{
		register(a, new SoundDeviceVisitor());
	}

	public void register(IMenusAware a)
	{
		register(a, new MenuDeviceVisitor());
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append("\n");
		for (AbstractDevice device : m_devices)
			sb.append("  ").append(device.toString().replaceAll("\\n", "\n  ")).append("\n");
		return sb.toString();
	}

	/**
	 * Group of devices that are associated to a location and activity tuple.
	 * 
	 * @author Sven Zethelius
	 * 
	 */
	public static interface IDeviceGroup
	{
		/**
		 * The list of {@link IPowered} instances
		 * 
		 * @return
		 */
		Collection<IPowered> getPowered();

		/**
		 * The active {@link ISound} instance
		 * 
		 * @return
		 */
		ISound getSound();

		/**
		 * The active {@link IPlayable}
		 * 
		 * @return
		 */
		IPlayable getPlayable();

		/**
		 * The map of menu name to {@link IMenu}
		 * 
		 * @return
		 */
		Map<String, IMenu> getMenus();
	}

	private static class DeviceGroup implements IDeviceGroup
	{
		private final Collection<IPowered> m_powereds = new LinkedHashSet<IPowered>(4);
		private final Map<String, IMenu> m_menus = new LinkedHashMap<String, IMenu>(4);
		private final Collection<SelectableTracker> m_selectables = new ArrayList<SelectableTracker>(4);
		private IPlayable m_playable;
		private ISound m_sound;
		
		public void addPowered(IPowered... power)
		{
			Collections.addAll(m_powereds, power);
		}

		@Override
		public Collection<IPowered> getPowered()
		{
			return m_powereds;
		}

		public void addSelectable(ISelectable selectable, IPowered powered, String selection)
		{
			if (!selectable.getOptions().contains(selection))
				throw new IllegalArgumentException("selection " + selection + " not an available option");
			m_selectables.add(new SelectableTracker(selectable, powered, selection));
		}

		public Collection<SelectableTracker> getSelectables()
		{
			return m_selectables;
		}

		public void addMenu(IMenu menu, String menuType)
		{
			m_menus.put(menuType, menu);
		}

		@Override
		public Map<String, IMenu> getMenus()
		{
			return m_menus;
		}

		public void setPlayable(IPlayable playable)
		{
			m_playable = playable;
		}

		@Override
		public IPlayable getPlayable()
		{
			return m_playable;
		}

		public void setSound(ISound sound)
		{
			m_sound = sound;
		}

		@Override
		public ISound getSound()
		{
			return m_sound;
		}
	}
	
	private static class SelectableTracker implements IChangeListener
	{
		private final ISelectable m_selectable;
		private final IPowered m_powered;
		private final String m_selection;

		public SelectableTracker(ISelectable selectable, IPowered powered, String selection)
		{
			super();
			m_selectable = selectable;
			m_powered = powered;
			m_selection = selection;
		}

		@Override
		public void stateChanged(Object target, String property)
		{
			if (m_powered.isPowered())
				m_selectable.setSelection(m_selection);
		}

		public void register()
		{
			m_powered.addChangeListener(this);
			if (m_powered.isPowered())
				stateChanged(m_powered, "powered");
		}
		
		public void unregister()
		{
			m_powered.removeChangeListener(this);
		}
	}

	private static abstract class DeviceVisitor<T>
	{
		private T m_t;

		public void setObject(T t)
		{
			m_t = t;
		}
		
		public void update(DeviceGroup group, DeviceGroup groupOld)
		{
			update(m_t, group, groupOld);
		}
		
		protected abstract void update(T t, DeviceGroup groupNew, DeviceGroup groupOld);
	}

	private static class PoweredDeviceVisitor extends DeviceVisitor<IPoweredAware>
	{
		@Override
		protected void update(IPoweredAware t, DeviceGroup groupNew, DeviceGroup groupOld)
		{
			if (!groupOld.getPowered().isEmpty())
				t.removePowered(groupOld.getPowered());
			if (!groupNew.getPowered().isEmpty())
				t.addPowered(groupNew.getPowered());
		}
	}

	private static class PlayableDeviceVisitor extends DeviceVisitor<IPlayableAware>
	{
		@Override
		protected void update(IPlayableAware t, DeviceGroup groupNew, DeviceGroup groupOld)
		{
			t.setPlayable(groupNew.getPlayable());
		}
	}

	private static class SoundDeviceVisitor extends DeviceVisitor<ISoundAware>
	{
		@Override
		protected void update(ISoundAware t, DeviceGroup groupNew, DeviceGroup groupOld)
		{
			t.setSound(groupNew.getSound());
		}
	}
	
	private static class MenuDeviceVisitor extends DeviceVisitor<IMenusAware>
	{
		@Override
		protected void update(IMenusAware t, DeviceGroup groupNew, DeviceGroup groupOld)
		{
			if (!groupOld.getMenus().isEmpty())
				t.removeMenus(groupOld.getMenus());
			if (!groupNew.getMenus().isEmpty())
				t.addMenus(groupNew.getMenus());
		}
	}

	private static class SelectableDeviceVisitor extends DeviceVisitor<Void>
	{
		@Override
		protected void update(Void t, DeviceGroup groupNew, DeviceGroup groupOld)
		{
			for (SelectableTracker tracker : groupOld.getSelectables())
				tracker.unregister();
			for (SelectableTracker tracker : groupNew.getSelectables())
				tracker.register();
		}
	}

}
