/**
 * 
 */
package svenz.remote.device.impl.pioneer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.spi.LoggingEvent;
import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.Test;
import svenz.remote.device.IMenu;
import svenz.remote.device.IPlayable;
import svenz.remote.device.impl.AbstractDeviceUnitTests;
import svenz.remote.device.ipremote.PlayableImpl;

/**
 * @author Sven Zethelius
 *
 */
public class PlayerBDP150DeviceUnitTests extends AbstractDeviceUnitTests<PlayerBDP150Device>
{
	@Override
	protected PlayerBDP150Device create()
	{
		return new PlayerBDP150Device();
	}

	@Override
	protected void init()
	{
		m_test.setBusyQueryDelay(100, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testDeviceListenerMatch() throws Exception
	{
		InetAddress localHost = InetAddress.getLocalHost();
		EasyMock.expect(m_device.getDeviceType()).andReturn("urn:pioneer-co-jp:device:PioControlServer:1").anyTimes();
		EasyMock.expect(m_device.getFriendlyName()).andReturn("BDP-150").anyTimes();
		EasyMock.expect(m_device.getModelName()).andReturn("BDP-150/UCXESM").anyTimes();
		EasyMock.expect(m_device.getIPPort()).andReturn(8005).anyTimes();
		EasyMock.expect(m_device.getUDN()).andReturn("TestUDN");

		EasyMock.expect(
				m_channelManager.connect(EasyMock.eq(m_address = new InetSocketAddress(localHost, 8005)),
						EasyMock.capture(m_callback))).andReturn(m_channel);

		expectWrite("?L\r"); // power query sent on connect
		m_control.replay();
		m_listener.getValue().deviceAdded(m_device, new InetSocketAddress(localHost, 1234));
		m_control.verify();
		m_control.reset();
		// response from ?L pending after this method
	}

	@Test
	public void testHandleResponsePowerQueryOnDVD() throws Exception
	{
		testDeviceListenerMatch();
		testCodedStatus(m_test.getPowered(), "powered", false, true, Arrays.asList("?P\r", "?D\r", "?A\r"),
				Arrays.asList("BDP-150", "P01", "112", "0000000"));
		assertEquals("dvd", m_test.getInput().getStatus());
	}

	@Test
	public void testHandleResponsePowerQueryOnDVDNoDisk() throws Exception
	{
		testDeviceListenerMatch();
		testCodedStatus(m_test.getPowered(), "powered", false, true, Arrays.asList("?P\r", "?D\r", "?A\r", "OP\r"),
				Arrays.asList("BDP-150", "P01", "0xx", "0000000", "R"));
		assertEquals("dvd", m_test.getInput().getStatus());
	}

	@Test
	public void testHandleResponsePowerQueryOnNetflix() throws Exception
	{
		testDeviceListenerMatch();
		testCodedStatus(m_test.getPowered(), "powered", false, true, Arrays.asList("?P\r", "?D\r", "?A\r"),
				Arrays.asList("BDP-150", "P01", "0xx", "E04"));
		assertEquals("netflix", m_test.getInput().getStatus());
	}

	@Test
	public void testHandleResponsePowerQueryOff() throws Exception
	{
		testDeviceListenerMatch();
		// ?L response: E04, so power should be off
		testCodedStatus(m_test.getPowered(), "powered", false, false, null, Arrays.asList("E04"));
	}

	@Test
	public void testHandleResponsePowerOn() throws Exception
	{
		testHandleResponsePowerQueryOff();

		m_writes.reset();
		expectWrite("PN\r"); // power on
		expectWrite("?L\r"); // first query fails
		expectWrite("?L\r"); // second query succeeds
		expectWrite("?P\r", "?D\r");
		m_control.replay();
		assertEquals(false, m_test.getPowered().isPowered());
		m_test.getPowered().powerOn();
		read("R");
		waitForCapture(m_writes, 2); // happens async on timer
		// TODO confirm error code
		read("E04");// error on ?L\r
		waitForCapture(m_writes, 3);// happens async on timer
		read("BDP-150"); // success on ?L\r
		read("P01");

		assertEquals(true, m_test.getPowered().isPowered());
		assertEquals(0, m_executor.getActiveCount());
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testHandleResponsePowerOnCorruptResponse() throws Exception
	{
		testHandleResponsePowerQueryOff();

		m_writes.reset();
		expectWrite("PN\r"); // power on
		expectWrite("?L\r"); // first query fails
		expectWrite("?L\r"); // second query succeeds
		expectWrite("?P\r", "?D\r", "?A\r", "?P\r");
		m_control.replay();
		assertEquals(false, m_test.getPowered().isPowered());
		m_test.getPowered().powerOn();
		read("R");
		waitForCapture(m_writes, 2); // happens async on timer
		// TODO confirm error code
		read("E04");// error on ?L\r
		waitForCapture(m_writes, 3);// happens async on timer
		read("BDP-150"); // success on ?L\r
		read("?r?"); // error that happens when the play query happens too soon after loading
		read("112", "002002000041", "P01");

		m_control.verify();
		m_control.reset();

		assertEquals(true, m_test.getPowered().isPowered());
		assertEquals(0, m_executor.getActiveCount());

		List<LoggingEvent> events = m_appender.getEvents();
		assertEquals(1, events.size());
		events.clear();
	}

	@Test
	public void testHandleResponsePowerOnError() throws Exception
	{
		testHandleResponsePowerQueryOnDVD();
		expectWrite("PN\r");

		m_control.replay();
		assertEquals(true, m_test.getPowered().isPowered());
		m_test.getPowered().powerOn();
		read("E04");

		assertEquals(true, m_test.getPowered().isPowered());
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testHandleResponsePowerOff() throws Exception
	{
		testHandleResponsePowerQueryOnDVD();

		expectWrite("PF\r");
		m_control.replay();
		assertEquals(true, m_test.getPowered().isPowered());
		m_test.getPowered().powerOff();
		read("R");
		assertEquals(false, m_test.getPowered().isPowered());
		assertEquals(null, m_test.getPlayable().getCurrentAction());
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testHandleResponsePowerOffError() throws Exception
	{
		testHandleResponsePowerQueryOff();

		expectWrite("PF\r");
		m_control.replay();
		assertEquals(false, m_test.getPowered().isPowered());
		m_test.getPowered().powerOff();
		read("E04");
		assertEquals(false, m_test.getPowered().isPowered());
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testPlayable() throws Exception
	{
		testHandleResponsePowerQueryOnDVD();

		PlayableImpl play = m_test.getPlayable();
		testPlayable(play, IPlayable.Action.Play, "/A181AF39/RU\r\n", "R", IPlayable.Action.Play);
		testPlayable(play, IPlayable.Action.Pause, "/A181AF3A/RU\r\n", "R", IPlayable.Action.Pause);
		testPlayable(play, IPlayable.Action.Stop, "/A181AF38/RU\r\n", "R", IPlayable.Action.Stop);
		testPlayable(play, IPlayable.Action.Play, "/A181AF39/RU\r\n", "R", IPlayable.Action.Play);
		testPlayable(play, IPlayable.Action.NextTrack, "/A181AF3D/RU\r\n", "R", IPlayable.Action.Play);
		testPlayable(play, IPlayable.Action.PreviousTrack, "/A181AF3E/RU\r\n", "R", IPlayable.Action.Play);
		testPlayable(play, IPlayable.Action.FastForward, "/A181AFE9/RU\r\n", "R", IPlayable.Action.Play);
		testPlayable(play, IPlayable.Action.Rewind, "/A181AFEA/RU\r\n", "R", IPlayable.Action.Play);
	}

	@Test
	public void testMenuDisk() throws Exception
	{
		testHandleResponsePowerQueryOnDVD();
		IMenu menu = m_test.getMenuDisk();

		testMenu(menu, IMenu.Action.Menu, "/A181AFB4/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Enter, "/A181AFEF/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Exit, "/A181AF20/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Left, "/A187FFFF/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Right, "/A186FFFF/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Down, "/A185FFFF/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Up, "/A184FFFF/RU\r\n", "R");
	}

	@Test
	public void testMenuMain() throws Exception
	{
		testHandleResponsePowerQueryOnDVD();
		IMenu menu = m_test.getMenuMain();

		testMenu(menu, IMenu.Action.Menu, "/A181AFB0/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Enter, "/A181AFEF/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Exit, "/A181AF20/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Left, "/A187FFFF/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Right, "/A186FFFF/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Down, "/A185FFFF/RU\r\n", "R");
		testMenu(menu, IMenu.Action.Up, "/A184FFFF/RU\r\n", "R");
	}

	@Test
	public void testNetflix() throws Exception
	{
		testHandleResponsePowerQueryOnDVD();

		expectWrite("/A181AF6A/RU\r\n");
		m_control.replay();
		m_test.getInput().setSelection("netflix");
		read("R");
		m_control.verify();
		m_control.reset();
	}

	@Test
	@Ignore
	public void testClose() throws Exception
	{
		fail(); // TODO
	}

	@Test
	@Ignore
	public void testCloseBusy() throws Exception
	{
		fail(); // TODO
	}

}
