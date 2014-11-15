/**
 * 
 */
package svenz.remote.ui;

import java.util.Arrays;
import java.util.concurrent.Executor;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;
import svenz.remote.device.IChangable.IChangeListener;
import svenz.remote.device.IPowered;
import svenz.test.helper.SynchronousExecutor;

/**
 * Assumes a background value of off.
 * 
 * @author Sven Zethelius
 * 
 */
public class PowerAnimatorUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final PowerAnimator m_animator = new PowerAnimator();
	private final Executor m_executor = new SynchronousExecutor();
	private final IClipImage m_ringOff = m_control.createMock("ringOff", IClipImage.class);
	private final IClipImage m_ringOn = m_control.createMock("ringOn", IClipImage.class);;
	private final IClipImage m_stickOff = m_control.createMock("stickOff", IClipImage.class);;
	private final IClipImage m_stickOn = m_control.createMock("stickOn", IClipImage.class);;
	private final IPowered[] m_powereds = { 
			m_control.createMock("Power1", IPowered.class),
			m_control.createMock("Power2", IPowered.class), 
			m_control.createMock("Power3", IPowered.class), };
	private final Capture<IChangeListener> m_changeListeners = new Capture<>(CaptureType.ALL);

	private enum DesiredState
	{
		On(0, 10000),
		Off(0, 0),
		Mixed(6900, 10000);
		
		private final int m_stickOn;
		private final int m_stickOff;

		private DesiredState(int stickOff, int stickOn)
		{
			m_stickOn = stickOn;
			m_stickOff = stickOff;
		}

		
	}
	
	@Before
	public void setup()
	{
		m_animator.setExecutor(m_executor);
		m_animator.setRingOff(m_ringOff);
		m_animator.setRingOn(m_ringOn);
		m_animator.setStickOff(m_stickOff);
		m_animator.setStickOn(m_stickOn);
	}

	private void testInit(DesiredState state, int ringOff, int ringOn, boolean... powereds)
	{
		addChangeListener(powereds.length);
		for (int i = 0; i < powereds.length; i++)
			isPowered(i, powereds[i]);

		expectLevels(state, ringOff, ringOn);

		addPowered(powereds.length);
	}

	@Test
	public void testInitOff1() throws Exception
	{
		testInit(DesiredState.Off, 10000, 0, false);
	}

	@Test
	public void testInitOn1() throws Exception
	{
		testInit(DesiredState.On, 0, 10000, true);
	}

	@Test
	public void testToggleOffToOn1() throws Exception
	{
		testInitOff1();
		m_powereds[0].powerOn();
		isPowered(0, true);

		expectLevels(DesiredState.On, 0, 10000);

		toggle();
	}

	@Test
	public void testToggleOnToOff1() throws Exception
	{
		testToggleOffToOn1();
		m_powereds[0].powerOff();
		isPowered(0, false);

		expectLevels(DesiredState.Off, 10000, 0);

		toggle();
	}

	@Test
	public void testToggleOffToOnDelayed1() throws Exception
	{
		testInitOff1();

		m_powereds[0].powerOn();
		isPowered(0, false);
		expectLevels(DesiredState.On, 0, 0);
		toggle();

		isPowered(0, true);
		expectLevels(DesiredState.On, 0, 10000);
		stateChanged(0);
	}

	@Test
	public void testToggleOnToOffDelayed1() throws Exception
	{
		testToggleOffToOn1();
		m_powereds[0].powerOff();
		isPowered(0, true);

		expectLevels(DesiredState.Off, 0, 10000);
		toggle();

		isPowered(0, false);
		expectLevels(DesiredState.Off, 10000, 0);
		stateChanged(0);
	}

	@Test
	public void testInitOff2() throws Exception
	{
		testInit(DesiredState.Off, 10000, 0, false, false);
	}

	@Test
	public void testInitOn2() throws Exception
	{
		testInit(DesiredState.On, 0, 10000, true, true);
	}

	@Test
	public void testInitMixed2() throws Exception
	{
		testInit(DesiredState.Mixed, 0, 5000, true, false);
	}

	@Test
	public void testInitOff3() throws Exception
	{
		testInit(DesiredState.Off, 10000, 0, false, false, false);
	}

	@Test
	public void testInitOn3() throws Exception
	{
		testInit(DesiredState.On, 0, 10000, true, true, true);
	}

	@Test
	public void testInitMixed3() throws Exception
	{
		testInit(DesiredState.Mixed, 0, 3333, false, true, false);
	}

	@Test
	public void testToggleOff2ToOn2Delayed() throws Exception
	{
		testInitOff2();

		m_powereds[0].powerOn();
		m_powereds[1].powerOn();
		isPowered(0, false);
		isPowered(1, false);
		expectLevels(DesiredState.On, 0, 0);
		toggle();

		isPowered(0, true);
		isPowered(1, false);
		expectLevels(DesiredState.On, 0, 5000);
		stateChanged(0);

		isPowered(0, true);
		isPowered(1, true);
		expectLevels(DesiredState.On, 0, 10000);
		stateChanged(0);

	}

	@Test
	public void testToggleOn2ToOff2Delayed() throws Exception
	{
		testInitOn2();
		m_powereds[0].powerOff();
		m_powereds[1].powerOff();
		isPowered(0, true);
		isPowered(1, true);

		expectLevels(DesiredState.Off, 0, 10000);
		toggle();

		isPowered(0, false);
		isPowered(1, true);
		expectLevels(DesiredState.Off, 5000, 10000);
		stateChanged(0);

		isPowered(0, false);
		isPowered(1, false);
		expectLevels(DesiredState.Off, 10000, 0);
		stateChanged(0);
	}

	@Test
	public void testToggleMixed2ToOn2Delayed() throws Exception
	{
		testInitMixed2();
		m_powereds[0].powerOn();
		m_powereds[1].powerOn();
		isPowered(0, false);
		isPowered(1, true);

		expectLevels(DesiredState.On, 0, 5000);
		toggle();

		isPowered(0, true);
		isPowered(1, true);
		expectLevels(DesiredState.On, 0, 10000);
		stateChanged(0);

	}

	@Test
	public void testChangeFromMixed3ToOn() throws Exception
	{
		testInitMixed3();

		isPowered(0, true);
		isPowered(1, true);
		isPowered(2, false);
		expectLevels(DesiredState.Mixed, 0, 6666);
		stateChanged(0);

		isPowered(0, true);
		isPowered(1, true);
		isPowered(2, true);
		expectLevels(DesiredState.On, 0, 10000);
		stateChanged(2);
	}

	@Test
	public void testChangeFromOn3ToOff3() throws Exception
	{
		testInitOn3();

		isPowered(0, true);
		isPowered(1, false);
		isPowered(2, true);
		expectLevels(DesiredState.On, 0, 6666);
		stateChanged(2);

		isPowered(0, false);
		isPowered(1, false);
		isPowered(2, true);
		expectLevels(DesiredState.On, 0, 3333);
		stateChanged(1);

		isPowered(0, false);
		isPowered(1, false);
		isPowered(2, false);
		expectLevels(DesiredState.Off, 10000, 0);
		stateChanged(2);
	}

	@Test
	public void testChangeFromOff3ToOn3() throws Exception
	{
		testInitOff3();

		isPowered(0, false);
		isPowered(1, false);
		isPowered(2, true);
		expectLevels(DesiredState.Off, 6666, 10000);
		stateChanged(2);

		isPowered(0, false);
		isPowered(1, true);
		isPowered(2, true);
		expectLevels(DesiredState.Off, 3333, 10000);
		stateChanged(1);

		isPowered(0, true);
		isPowered(1, true);
		isPowered(2, true);
		expectLevels(DesiredState.On, 0, 10000);
		stateChanged(0);
	}

	@Test
	public void testPowerOff3ToOff2() throws Exception
	{
		testInitOff3();

		removeChangeListener(3);
		addChangeListener(2);
		isPowered(0, false);
		isPowered(1, false);

		expectLevels(DesiredState.Off, 10000, 0);

		m_control.replay();
		m_animator.removePowered(Arrays.asList(m_powereds).subList(0, 3));
		m_animator.addPowered(Arrays.asList(m_powereds).subList(0, 2));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testPowerOn3ToOn2() throws Exception
	{
		testInitOn3();

		removeChangeListener(3);
		addChangeListener(2);
		isPowered(0, true);
		isPowered(1, true);

		expectLevels(DesiredState.On, 0, 10000);

		m_control.replay();
		m_animator.removePowered(Arrays.asList(m_powereds).subList(0, 3));
		m_animator.addPowered(Arrays.asList(m_powereds).subList(0, 2));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testPowerOn2ToOn3() throws Exception
	{
		testInitOn2();

		removeChangeListener(2);
		addChangeListener(3);
		isPowered(0, true);
		isPowered(1, true);
		isPowered(2, true);

		expectLevels(DesiredState.On, 0, 10000);

		m_control.replay();
		m_animator.removePowered(Arrays.asList(m_powereds).subList(0, 2));
		m_animator.addPowered(Arrays.asList(m_powereds).subList(0, 3));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testPowerOff2ToOff3() throws Exception
	{
		testInitOff2();

		removeChangeListener(2);
		addChangeListener(3);
		isPowered(0, false);
		isPowered(1, false);
		isPowered(2, false);

		expectLevels(DesiredState.Off, 10000, 0);

		m_control.replay();
		m_animator.removePowered(Arrays.asList(m_powereds).subList(0, 2));
		m_animator.addPowered(Arrays.asList(m_powereds).subList(0, 3));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testPowerOff2ToMixed3() throws Exception
	{
		testInitOff2();

		removeChangeListener(2);
		addChangeListener(3);
		isPowered(0, false);
		isPowered(1, false);
		isPowered(2, true);

		expectLevels(DesiredState.Mixed, 0, 3333);

		m_control.replay();
		m_animator.removePowered(Arrays.asList(m_powereds).subList(0, 2));
		m_animator.addPowered(Arrays.asList(m_powereds).subList(0, 3));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testPowerOn2ToMixed3() throws Exception
	{
		testInitOff2();

		removeChangeListener(2);
		addChangeListener(3);
		isPowered(0, true);
		isPowered(1, true);
		isPowered(2, false);

		expectLevels(DesiredState.Mixed, 0, 6666);

		m_control.replay();
		m_animator.removePowered(Arrays.asList(m_powereds).subList(0, 2));
		m_animator.addPowered(Arrays.asList(m_powereds).subList(0, 3));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testPowerMixed2ToMixed3() throws Exception
	{
		testInitOff2();

		removeChangeListener(2);
		addChangeListener(3);
		isPowered(0, true);
		isPowered(1, false);
		isPowered(2, false);

		expectLevels(DesiredState.Mixed, 0, 3333);

		m_control.replay();
		m_animator.removePowered(Arrays.asList(m_powereds).subList(0, 2));
		m_animator.addPowered(Arrays.asList(m_powereds).subList(0, 3));
		m_control.verify();
		m_control.reset();
	}

	private void addChangeListener(int count)
	{
		for (int i = 0; i < count; i++)
			m_powereds[i].addChangeListener(EasyMock.capture(m_changeListeners));
	}

	private void removeChangeListener(int count)
	{
		for (int i = 0; i < count; i++)
			m_powereds[i].removeChangeListener(m_animator);
	}

	private void stateChanged(int num)
	{
		m_control.replay();
		m_changeListeners.getValues().get(num).stateChanged(m_powereds[num], "powered");
		m_control.verify();
		m_control.reset();
	}

	private void expectLevels(DesiredState state, int ringOff, int ringOn)
	{
		// off masks on, so check off for full value
		m_stickOff.setLevel(state.m_stickOff);
		m_stickOn.setLevel(state.m_stickOff == 10000 ? EasyMock.anyInt() : state.m_stickOn);
		m_ringOff.setLevel(ringOff);
		m_ringOn.setLevel(ringOff == 10000 ? EasyMock.anyInt() : ringOn);
	}



	private void toggle()
	{
		m_control.replay();
		m_animator.toggle();
		m_control.verify();
		m_control.reset();
	}

	private void addPowered(int count)
	{
		m_control.replay();
		m_animator.addPowered(Arrays.asList(m_powereds).subList(0, count));
		m_control.verify();
		m_control.reset();
	}


	private void isPowered(int num, boolean powered)
	{
		EasyMock.expect(m_powereds[num].isPowered()).andReturn(powered);
	}

}
