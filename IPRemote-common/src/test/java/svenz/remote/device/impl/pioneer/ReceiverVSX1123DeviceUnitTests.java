/**
 * 
 */
package svenz.remote.device.impl.pioneer;

import static org.junit.Assert.assertEquals;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import org.easymock.EasyMock;
import org.junit.Test;
import svenz.remote.device.impl.AbstractDeviceUnitTests;
import svenz.remote.device.ipremote.PoweredImpl;
import svenz.remote.device.ipremote.SelectableImpl;
import svenz.remote.device.ipremote.SoundImpl;

/**
 * 
 * Mock Test for Receiver
 * 
 * @author Sven Zethelius
 * 
 */
public class ReceiverVSX1123DeviceUnitTests extends AbstractDeviceUnitTests<ReceiverVSX1123Device>
{
	@Override
	protected ReceiverVSX1123Device create()
	{
		return new ReceiverVSX1123Device();
	}

	@Test
	public void testDeviceListenerMatch() throws Exception
	{
		InetAddress localHost = InetAddress.getLocalHost();
		EasyMock.expect(m_device.getDeviceType()).andReturn("urn:schemas-upnp-org:device:MediaRenderer:1").anyTimes();
		EasyMock.expect(m_device.getFriendlyName()).andReturn("VSX-1123").anyTimes();
		EasyMock.expect(m_device.getModelName()).andReturn("VSX-1123/CUXESM").anyTimes();
		EasyMock.expect(m_device.getIPPort()).andReturn(8005).anyTimes();
		EasyMock.expect(m_device.getUDN()).andReturn("TestUDN");

		EasyMock.expect(
				m_channelManager.connect(EasyMock.eq(m_address = new InetSocketAddress(localHost, 8005)),
						EasyMock.capture(m_callback))).andReturn(m_channel);

		expectWrite("?P\r", "?V\r", "?M\r", "?F\r", "?S\r");
		expectWrite("?AP\r", "?ZV\r", "?Z2M\r", "?ZS\r");
		expectWrite("?ZEP\r", "?ZEA\r");
		expectWrite("?FL\r");

		m_control.replay();
		m_listener.getValue().deviceAdded(m_device, new InetSocketAddress(localHost, 1234));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testZone1StatusPower() throws Exception
	{
		testDeviceListenerMatch();
		testCodedStatus(m_test.getZone1().getPowered(), "powered", false, true,
				Arrays.asList("?F\r", "?V\r", "?M\r", "?S\r"), Arrays.asList("PWR0"));
		testCodedStatus(m_test.getZone1().getPowered(), "powered", true, false, null,
				Arrays.asList("PWR1"));
	}

	@Test
	public void testZone2StatusPower() throws Exception
	{
		testDeviceListenerMatch();
		testCodedStatus(m_test.getZone2().getPowered(), "powered", false, true,
				Arrays.asList("?ZV\r", "?Z2M\r", "?ZS\r"), Arrays.asList("APR0"));
		testCodedStatus(m_test.getZone2().getPowered(), "powered", true, false, 
				null, Arrays.asList("APR1"));
	}

	@Test
	public void testZone3StatusPower() throws Exception
	{
		testDeviceListenerMatch();
		testCodedStatus(m_test.getZoneHDMI().getPowered(), "powered", false, true, Arrays.asList("?ZEA\r"),
				Arrays.asList("ZEP0"));
		testCodedStatus(m_test.getZoneHDMI().getPowered(), "powered", true, false, null, Arrays.asList("ZEP1"));
	}

	@Test
	public void testZone1Power() throws Exception
	{
		testPower(m_test.getZone1().getPowered(), "PWR0", "PWR1", "PO\r", "?V\r", "?M\r", "?F\r", "?S\r", "PF\r");
	}

	@Test
	public void testZone2Power() throws Exception
	{
		testPower(m_test.getZone2().getPowered(), "APR0", "APR1", "APO\r", "?ZV\r", "?Z2M\r", "?ZS\r", "APF\r");
	}

	@Test
	public void testZone3PowerOn() throws Exception
	{
		testPower(m_test.getZoneHDMI().getPowered(), "ZEP0", "ZEP1", "ZEO\r", "?ZEA\r", "ZEF\r");
	}

	@Test
	public void testZone1StatusVolume() throws Exception
	{
		testDeviceListenerMatch();
		testCodedStatus(m_test.getZone1().getSound(), "volume", -1, 54, null, Arrays.asList("VOL054"));
	}

	@Test
	public void testZone2StatusVolume() throws Exception
	{
		testDeviceListenerMatch();
		testCodedStatus(m_test.getZone2().getSound(), "volume", -1, 42, null, Arrays.asList("ZV042"));
	}

	@Test
	public void testZone1SetVolume() throws Exception
	{
		testSetVolume(m_test.getZone1().getSound(), 44, "VOL044", "044VL\r");
	}

	@Test
	public void testZone2SetVolume() throws Exception
	{
		testSetVolume(m_test.getZone2().getSound(), 34, "ZV034", "034ZV\r");
	}

	@Test
	public void testZone1StatusMute() throws Exception
	{
		testDeviceListenerMatch();
		testCodedStatus(m_test.getZone1().getSound(), "mute", false, true, null, Arrays.asList("MUT0\r"));
		testCodedStatus(m_test.getZone1().getSound(), "mute", true, false, null, Arrays.asList("MUT1\r"));
	}

	@Test
	public void testZone2StatusMute() throws Exception
	{
		testDeviceListenerMatch();
		testCodedStatus(m_test.getZone2().getSound(), "mute", false, true, null, Arrays.asList("Z2MUT0\r"));
		testCodedStatus(m_test.getZone2().getSound(), "mute", true, false, null, Arrays.asList("Z2MUT1\r"));
	}

	@Test
	public void testZone1Mute() throws Exception
	{
		testMute(m_test.getZone1().getSound(), "MUT0", "MUT1", "MO\r", "MF\r");
	}

	@Test
	public void testZone2Mute() throws Exception
	{
		testMute(m_test.getZone2().getSound(), "Z2MUT0", "Z2MUT1", "Z2MO\r", "Z2MF\r");
	}

	@Test
	public void testZone1Input() throws Exception
	{
		testDeviceListenerMatch();
		testInput(m_test.getZone1().getInput(), "25", "FN25", "25FN\r");
		testInput(m_test.getZone1().getInput(), "24", "FN24", "24FN\r");
		testInput(m_test.getZone1().getInput(), "02", "FN02", "02FN\r");
	}

	@Test
	public void testZone2Input() throws Exception
	{
		testDeviceListenerMatch();
		testInput(m_test.getZone2().getInput(), "25", "Z2F25", "25ZS\r");
		testInput(m_test.getZone2().getInput(), "24", "Z2F24", "24ZS\r");
		testInput(m_test.getZone2().getInput(), "02", "Z2F02", "02ZS\r");
	}

	@Test
	public void testZone3Input() throws Exception
	{
		testDeviceListenerMatch();
		testInput(m_test.getZoneHDMI().getInput(), "25", "ZEA25", "25ZEA\r");
		testInput(m_test.getZoneHDMI().getInput(), "24", "ZEA24", "24ZEA\r");
		testInput(m_test.getZoneHDMI().getInput(), "02", "ZEA02", "02ZEA\r");
	}

	@Test
	public void testZone1ListeningMode() throws Exception
	{
		testDeviceListenerMatch();
		m_control.replay();
		read("LM0401"); // no errors
		m_control.verify();
		m_control.reset();
	}

	// TODO test
	// Display
	// Listening Mode ?S values
	// Audio parameters
	// Video parameters
	// Listening Mode when not powered: E02

	private void testPower(PoweredImpl powered, String readOn, String readOff, String... writes) throws Exception
	{
		testDeviceListenerMatch();
		expectWrite(writes);
		m_control.replay();
		assertEquals(false, powered.isPowered());
		powered.powerOn();
		read(readOn);
		assertEquals(true, powered.isPowered());
		powered.powerOff();
		read(readOff);
		assertEquals(false, powered.isPowered());
		m_control.verify();
		m_control.reset();
	}

	private void testSetVolume(SoundImpl sound, int vol, String read, String... writes) throws Exception
	{
		testDeviceListenerMatch();

		expectWrite(writes);
		m_control.replay();
		assertEquals(-1, sound.getVolume());
		sound.setVolume(vol);
		read(read);
		assertEquals(vol, sound.getVolume());
		m_control.verify();
		m_control.reset();
	}

	private void testMute(SoundImpl sound, String readOn, String readOff, String... writes) throws Exception
	{
		testDeviceListenerMatch();

		expectWrite(writes);
		m_control.replay();
		assertEquals(false, sound.isMute());
		sound.mute(true);
		read(readOn);
		assertEquals(true, sound.isMute());
		sound.mute(false);
		read(readOff);
		assertEquals(false, sound.isMute());
		m_control.verify();
		m_control.reset();
	}

	private void testInput(SelectableImpl input, String select, String read, String... writes) throws Exception
	{
		expectWrite(writes);
		m_control.replay();
		input.setSelection(select);
		read(read);
		assertEquals(select, input.getSelection());
		m_control.verify();
		m_control.reset();
	}
}
