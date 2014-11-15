/**
 * 
 */
package svenz.remote.device;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import svenz.remote.device.IChangable.IChangeListener;
import svenz.remote.device.IPlayable.IPlayableAware;
import svenz.remote.device.IPowered.IPoweredAware;
import svenz.remote.device.ISound.ISoundAware;
import svenz.remote.device.ipremote.AbstractDevice;
import svenz.remote.device.ipremote.ChannelDeviceListener;
import svenz.remote.device.jaxb.Device;
import svenz.remote.device.jaxb.DeviceGroups;
import svenz.remote.device.jaxb.DeviceReference;
import svenz.remote.device.jaxb.Group;
import svenz.remote.device.jaxb.MenuReference;
import svenz.remote.device.jaxb.SelectableReference;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.protocol.ssdp.SSDPManager;
import svenz.remote.net.protocol.ssdp.SSDPManager.IDeviceListener;

/**
 * @author Sven Zethelius
 * 
 */
public class DeviceGroupRegistryUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final SocketChannelManager m_channelManager = 
			m_control.createMock("SocketChannelManager", SocketChannelManager.class);
	private final SSDPManager m_ssdpManager = m_control.createMock("SSDPManager", SSDPManager.class);
	private final ScheduledExecutorService m_executor = 
			m_control.createMock("Executor", ScheduledExecutorService.class);

	private final DeviceGroupRegistry m_registry = new DeviceGroupRegistry();
	private final MockDevice m_device = new MockDevice();

	@Before
	public void setup()
	{
		m_control.replay();
		m_registry.setExecutor(m_executor);
		m_registry.setSSDPManager(m_ssdpManager);
		m_registry.setChannelManager(m_channelManager);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testLoad() throws Exception
	{
		DeviceGroups deviceGroups = new DeviceGroups();
		deviceGroups.getDevices().add(device("D", m_device));
		deviceGroups.getGroups().add(
				group("l1", "a1", ref("D.playable1"), ref("D.sound1"),
						Arrays.asList(mref("D.menu1", "disk"), mref("D.menu2", "main")),
						Arrays.asList(sref("D.selectable1", "netflix"))));
		deviceGroups.getGroups().add(
				group("l2", "a2", ref("D.playable2"), ref("D.sound2"),
						Arrays.asList(mref("D.menu2", "disk"), mref("D.menu2", "main")),
						Arrays.asList(sref("D.selectable2", "netflix"))));

		EasyMock.expect(m_device.getSelectable1().getOptions()).andReturn(Arrays.asList("dvd", "netflix"));
		EasyMock.expect(m_device.getSelectable2().getOptions()).andReturn(Arrays.asList("radio", "netflix"));
		m_control.replay();
		m_registry.load(deviceGroups);
		assertEquals(new HashSet<String>(Arrays.asList("l1", "l2", "none")), m_registry.getLocations());
		assertEquals(new HashSet<String>(Arrays.asList("a1", "a2", "none")), m_registry.getActivities());
		assertEquals(new HashSet<String>(Arrays.asList("l1")), m_registry.getLocationsForActivity("a1"));
		assertEquals(new HashSet<String>(Arrays.asList("l2")), m_registry.getLocationsForActivity("a2"));
		assertEquals(new HashSet<String>(Arrays.asList("a1")), m_registry.getActivitiesForLocation("l1"));
		assertEquals(new HashSet<String>(Arrays.asList("a2")), m_registry.getActivitiesForLocation("l2"));
		assertEquals(Collections.EMPTY_SET, m_registry.getLocationsForActivity("a0"));
		assertEquals(Collections.EMPTY_SET, m_registry.getActivitiesForLocation("l0"));

		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testOpen() throws Exception
	{
		testLoad();

		m_ssdpManager.addDeviceListener(EasyMock.isA(IDeviceListener.class));

		m_control.replay();
		m_registry.open();
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testMultipleOpen() throws Exception
	{
		testOpen();

		m_ssdpManager.removeDeviceListener(EasyMock.isA(IDeviceListener.class));
		m_ssdpManager.addDeviceListener(EasyMock.isA(IDeviceListener.class));
		m_control.replay();
		m_device.close();
		m_registry.open();
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLoadBadClass() throws Exception
	{
		DeviceGroups deviceGroups = new DeviceGroups();
		Device d = m_control.createMock("Device", Device.class);
		EasyMock.expect(d.getName()).andReturn("D").anyTimes();
		EasyMock.expect(d.getImplInstance()).andThrow(new InstantiationException("Test Exception")).anyTimes();

		deviceGroups.getDevices().add(d);
		deviceGroups.getGroups().add(
				group("l1", "a1", ref("D.playable1"), ref("D.sound1"),
						Arrays.asList(mref("D.menu1", "disk"), mref("D.menu2", "main")),
						Arrays.asList(sref("D.selectable1", "netflix"))));

		m_control.replay();
		m_registry.load(deviceGroups);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testActiveDeviceGroup() throws Exception
	{
		IPlayableAware playAware = m_control.createMock("PlayableAware", IPlayableAware.class);
		IPoweredAware powerAware = m_control.createMock("PowerAware", IPoweredAware.class);
		ISoundAware soundAware = m_control.createMock("SoundAware", ISoundAware.class);

		m_control.replay();
		m_registry.register(playAware);
		m_registry.register(powerAware);
		m_registry.register(soundAware);
		m_control.verify();
		m_control.reset();

		testLoad();

		Capture<IChangeListener> captureChange = new Capture<IChangeListener>(CaptureType.ALL);
		EasyMock.expect(m_device.getPowered().isPowered()).andReturn(false);
		m_device.getPowered().addChangeListener(EasyMock.capture(captureChange));
		powerAware.addPowered(Collections.singleton(m_device.getPowered()));
		playAware.setPlayable(m_device.getPlayable1());
		soundAware.setSound(m_device.getSound1());
		m_control.replay();
		m_registry.setActiveDeviceGroup("l1", "a1");
		m_control.verify();
		m_control.reset();

		m_device.getPowered().removeChangeListener(captureChange.getValue());
		powerAware.removePowered(Collections.singleton(m_device.getPowered()));
		playAware.setPlayable(null);
		soundAware.setSound(null);
		m_control.replay();
		m_registry.setActiveDeviceGroup("l2", "a1");
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadSelectableValue() throws Exception
	{
		DeviceGroups deviceGroups = new DeviceGroups();
		deviceGroups.getDevices().add(device("D", m_device));
		deviceGroups.getGroups().add(
				group("l1", "a1", ref("D.playable1"), ref("D.sound1"),
						Arrays.asList(mref("D.menu1", "disk"), mref("D.menu2", "main")),
						Arrays.asList(sref("D.selectable1", "other"))));

		m_ssdpManager.addDeviceListener(EasyMock.isA(IDeviceListener.class));
		EasyMock.expect(m_device.getSelectable1().getOptions()).andReturn(Arrays.asList("dvd", "netflix"));
		m_control.replay();
		m_registry.load(deviceGroups);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSelectableAlreadyPowered() throws Exception
	{
		IPlayableAware playAware = m_control.createMock("PlayableAware", IPlayableAware.class);
		IPoweredAware powerAware = m_control.createMock("PowerAware", IPoweredAware.class);
		ISoundAware soundAware = m_control.createMock("SoundAware", ISoundAware.class);

		m_control.replay();
		m_registry.register(playAware);
		m_registry.register(powerAware);
		m_registry.register(soundAware);
		m_control.verify();
		m_control.reset();

		testLoad();

		Capture<IChangeListener> captureChange = new Capture<IChangeListener>(CaptureType.ALL);
		EasyMock.expect(m_device.getPowered().isPowered()).andReturn(true).atLeastOnce();
		m_device.getPowered().addChangeListener(EasyMock.capture(captureChange));
		m_device.getSelectable1().setSelection("netflix");
		powerAware.addPowered(Collections.singleton(m_device.getPowered()));
		playAware.setPlayable(m_device.getPlayable1());
		soundAware.setSound(m_device.getSound1());
		m_control.replay();
		m_registry.setActiveDeviceGroup("l1", "a1");
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSelectableAfterPowered() throws Exception
	{
		IPlayableAware playAware = m_control.createMock("PlayableAware", IPlayableAware.class);
		IPoweredAware powerAware = m_control.createMock("PowerAware", IPoweredAware.class);
		ISoundAware soundAware = m_control.createMock("SoundAware", ISoundAware.class);

		m_control.replay();
		m_registry.register(playAware);
		m_registry.register(powerAware);
		m_registry.register(soundAware);
		m_control.verify();
		m_control.reset();

		testLoad();

		Capture<IChangeListener> captureChange = new Capture<IChangeListener>(CaptureType.ALL);
		EasyMock.expect(m_device.getPowered().isPowered()).andReturn(false).atLeastOnce();
		m_device.getPowered().addChangeListener(EasyMock.capture(captureChange));
		powerAware.addPowered(Collections.singleton(m_device.getPowered()));
		playAware.setPlayable(m_device.getPlayable1());
		soundAware.setSound(m_device.getSound1());
		m_control.replay();
		m_registry.setActiveDeviceGroup("l1", "a1");
		m_control.verify();
		m_control.reset();

		EasyMock.expect(m_device.getPowered().isPowered()).andReturn(true);
		m_device.getSelectable1().setSelection("netflix");
		m_control.replay();
		captureChange.getValue().stateChanged(m_device.getPowered(), "powered");
		m_control.verify();
		m_control.reset();
	}


	private Device device(String name, MockDevice m) throws Exception
	{
		Device d = m_control.createMock("Device", Device.class);
		EasyMock.expect(d.getName()).andReturn(name).anyTimes();
		EasyMock.expect(d.getImplInstance()).andReturn(m).anyTimes();
		return d;
	}

	private Group group(String location, String activity, 
			DeviceReference play, 
			DeviceReference sound,
			Collection<MenuReference> menus,
			Collection<SelectableReference> selects)
	{
		Group g = new Group();
		g.setLocation(location);
		g.setActivity(activity);
		g.setPlayable(play);
		g.setSound(sound);
		g.getMenuReferences().addAll(menus);
		g.getSelectableReferences().addAll(selects);
		return g;
	}

	private DeviceReference ref(String ref)
	{
		DeviceReference r = new DeviceReference();
		r.setReference(ref);
		return r;
	}

	private MenuReference mref(String ref, String name)
	{
		MenuReference r = new MenuReference();
		r.setName(name);
		r.setReference(ref);
		return r;
	}

	private SelectableReference sref(String ref, String value)
	{
		SelectableReference r = new SelectableReference();
		r.setValue(value);
		r.setReference(ref);
		return r;
	}

	public class MockDevice extends AbstractDevice
	{
		private final IPowered m_powered = m_control.createMock("Powered", IPowered.class);
		private final IPlayable m_playable1 = m_control.createMock("Playable1", IPlayable.class);
		private final IPlayable m_playable2 = m_control.createMock("Playable2", IPlayable.class);
		private final ISound m_sound1 = m_control.createMock("Sound1", ISound.class);
		private final ISound m_sound2 = m_control.createMock("Sound2", ISound.class);
		private final IMenu m_menu1 = m_control.createMock("Menu1", IMenu.class);
		private final IMenu m_menu2 = m_control.createMock("Menu2", IMenu.class);
		private final ISelectable m_selectable1 = m_control.createMock("Selectable1", ISelectable.class);
		private final ISelectable m_selectable2 = m_control.createMock("Selectable2", ISelectable.class);
		private final List<String> m_responses = new ArrayList<String>();
		@Override
		protected IDeviceListener<?> initDeviceListener(ChannelDeviceListener listener)
		{
			return listener;
		}

		@Override
		protected void handleResponse(String response)
		{
			synchronized (m_responses)
			{
				m_responses.add(response);
			}
		}

		public IPowered getPowered()
		{
			return m_powered;
		}

		public IPlayable getPlayable1()
		{
			return m_playable1;
		}

		public IPlayable getPlayable2()
		{
			return m_playable2;
		}

		public ISound getSound1()
		{
			return m_sound1;
		}

		public ISound getSound2()
		{
			return m_sound2;
		}

		public IMenu getMenu1()
		{
			return m_menu1;
		}

		public IMenu getMenu2()
		{
			return m_menu2;
		}

		public ISelectable getSelectable1()
		{
			return m_selectable1;
		}

		public ISelectable getSelectable2()
		{
			return m_selectable2;
		}

		public List<String> getResponses()
		{
			return m_responses;
		}
	}

}
