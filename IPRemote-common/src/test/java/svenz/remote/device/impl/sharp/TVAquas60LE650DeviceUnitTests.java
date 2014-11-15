/**
 * 
 */
package svenz.remote.device.impl.sharp;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import org.easymock.EasyMock;
import org.junit.Test;
import svenz.remote.device.IMenu;
import svenz.remote.device.IPlayable;
import svenz.remote.device.IPlayable.Action;
import svenz.remote.device.IPowered;
import svenz.remote.device.ISelectable;
import svenz.remote.device.impl.AbstractDeviceUnitTests;

/**
 * @author Sven Zethelius
 *
 */
public class TVAquas60LE650DeviceUnitTests extends AbstractDeviceUnitTests<TVAquas60LE650Device>
{

	@Override
	protected TVAquas60LE650Device create()
	{
		return new TVAquas60LE650Device();
	}

	@Test
	public void testDeviceListenerMatch() throws Exception
	{
		InetAddress localHost = InetAddress.getLocalHost();
		int port = m_test.getIPRemotePort();

		EasyMock.expect(m_device.getDeviceType()).andReturn("urn:schemas-sharp-co-jp:device:AquosIPC:1").anyTimes();
		EasyMock.expect(m_device.getFriendlyName()).andReturn("AQUOS LE650U").anyTimes();
		EasyMock.expect(m_device.getModelName()).andReturn("AQUOS \nLC-60LE650U").anyTimes();
		EasyMock.expect(m_device.getIPPort()).andReturn(port).anyTimes();
		EasyMock.expect(m_device.getUDN()).andReturn("TestUDN").anyTimes();

		EasyMock.expect(
				m_channelManager.connect(EasyMock.eq(m_address = new InetSocketAddress(localHost, port)),
						EasyMock.capture(m_callback))).andReturn(m_channel);

		expectWrite("TVNM1   \r"); // power query sent on connect
		m_control.replay();
		m_listener.getValue().deviceAdded(m_device, new InetSocketAddress(localHost, 1234));
		read("AQUOS LE650U");
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testDeviceListenerMatchNetflix() throws Exception
	{
		InetAddress localHost = InetAddress.getLocalHost();
		int port = m_test.getIPRemotePort();

		EasyMock.expect(m_device.getDeviceType()).andReturn(null).anyTimes();
		EasyMock.expect(m_device.getFriendlyName()).andReturn(null).anyTimes();
		EasyMock.expect(m_device.getModelName()).andReturn(null).anyTimes();
		EasyMock.expect(m_device.getIPPort()).andReturn(null).anyTimes();
		EasyMock.expect(m_device.getUDN()).andReturn(null).anyTimes();

		EasyMock.expect(
				m_channelManager.connect(EasyMock.eq(m_address = new InetSocketAddress(localHost, port)),
						EasyMock.capture(m_callback))).andReturn(m_channel);

		expectWrite("TVNM1   \r"); // power query sent on connect
		m_control.replay();
		m_listener.getValue().deviceAdded(m_device, new InetSocketAddress(localHost, 8090));
		read("AQUOS LE650U");
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testConfig() throws Exception
	{
		ISelectable input = m_test.getInput();
		assertEquals(new HashSet<String>(Arrays.asList("netflix", "youtube", "hdmi.1")), new HashSet<String>(input.getOptions()));
	}

	@Test
	public void testPower() throws Exception
	{
		testDeviceListenerMatch();
		IPowered powered = m_test.getPowered();
		assertEquals(true, powered.isPowered());

		expectWrite("POWR0   \r");
		m_control.replay();
		powered.powerOff();
		read("OK");
		m_control.verify();
		m_control.reset();

		assertEquals(false, powered.isPowered());

		expectWrite("POWR1   \r");
		m_control.replay();
		powered.powerOn();
		read("OK");
		m_control.verify();
		m_control.reset();

	}

	@Test
	public void testMenu() throws Exception
	{
		testDeviceListenerMatch();
		IMenu menu = m_test.getMenu();

		testMenu(menu, IMenu.Action.Menu, "RCKY38  \r", "OK");
		testMenu(menu, IMenu.Action.Enter, "RCKY40  \r", "OK");
		testMenu(menu, IMenu.Action.Exit, "RCKY46  \r", "OK");
		testMenu(menu, IMenu.Action.Left, "RCKY43  \r", "OK");
		testMenu(menu, IMenu.Action.Right, "RCKY44  \r", "OK");
		testMenu(menu, IMenu.Action.Down, "RCKY42  \r", "OK");
		testMenu(menu, IMenu.Action.Up, "RCKY41  \r", "OK");
	}

	@Test
	public void testPlayable() throws Exception
	{
		testDeviceListenerMatch();
		IPlayable play = m_test.getPlayable();
		testPlayable(play, IPlayable.Action.Play, "RCKY16  \r", "OK", IPlayable.Action.Play);
		testPlayable(play, IPlayable.Action.Pause, "RCKY18  \r", "OK", IPlayable.Action.Pause);
		testPlayable(play, IPlayable.Action.Stop, "RCKY20  \r", "OK", IPlayable.Action.Stop);
		testPlayable(play, IPlayable.Action.Play, "RCKY16  \r", "OK", IPlayable.Action.Play);
		testPlayable(play, IPlayable.Action.NextTrack, "RCKY21  \r", "OK", IPlayable.Action.Play);
		testPlayable(play, IPlayable.Action.PreviousTrack, "RCKY19  \r", "OK", IPlayable.Action.Play);
		testPlayable(play, IPlayable.Action.FastForward, "RCKY17  \r", "OK", IPlayable.Action.Play);
		testPlayable(play, IPlayable.Action.Rewind, "RCKY15  \r", "OK", IPlayable.Action.Play);
	}

	@Override
	protected void testPlayable(IPlayable playable, Action action, String code, String response, Action expected)
			throws IOException
	{
		super.testPlayable(playable, action, code, response, null); // TODO handle expected
	}
}
