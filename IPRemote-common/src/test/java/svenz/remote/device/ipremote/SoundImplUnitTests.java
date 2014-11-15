/**
 * 
 */
package svenz.remote.device.ipremote;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import org.apache.commons.configuration.MapConfiguration;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;
import svenz.remote.device.IChangable.IChangeListener;

/**
 * @author Sven Zethelius
 *
 */
public class SoundImplUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final SoundImpl m_sound = new SoundImpl();
	private final WritableByteChannel m_writer = m_control.createMock("Channel", WritableByteChannel.class);

	@Test
	public void testChangeListener()
	{
		MapConfiguration config = getConfig();
		
		IChangeListener listener = m_control.createMock("Listener", IChangeListener.class);
		listener.stateChanged(m_sound, "mute");
		listener.stateChanged(m_sound, "volume");

		m_control.replay();

		m_sound.setCodes(config);
		m_sound.addChangeListener(listener);

		m_sound.setMuteStatus(false);
		m_sound.setVolumeStatus(5);

		m_sound.setMuteStatus(false);
		m_sound.setVolumeStatus(5);

		m_sound.removeChangeListener(listener);

		m_sound.setMuteStatus(true);
		m_sound.setVolumeStatus(10);

		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetWriter() throws IOException
	{
		MapConfiguration config = getConfig();

		expectWrite("?VOL?");
		expectWrite("?MUTE?");
		m_control.replay();
		m_sound.setCodes(config);
		m_sound.setWriter(m_writer);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetVolume() throws IOException
	{
		MapConfiguration config = getConfig();

		expectWrite("?VOL?");
		expectWrite("?MUTE?");
		expectWrite("VOL10VOL");
		m_control.replay();
		m_sound.setCodes(config);
		m_sound.setWriter(m_writer);
		m_sound.setVolume(10);
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetVolumeUnder() throws Exception
	{
		MapConfiguration config = getConfig();

		expectWrite("?VOL?");
		expectWrite("?MUTE?");

		m_control.replay();
		m_sound.setCodes(config);
		m_sound.setWriter(m_writer);
		m_sound.setVolumeMin(5);
		m_sound.setVolume(1);
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetVolumeAbove() throws Exception
	{
		MapConfiguration config = getConfig();

		expectWrite("?VOL?");
		expectWrite("?MUTE?");

		m_control.replay();
		m_sound.setCodes(config);
		m_sound.setWriter(m_writer);
		m_sound.setVolume(101);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetVolumeMuted() throws IOException
	{
		MapConfiguration config = getConfig();

		expectWrite("?VOL?");
		expectWrite("?MUTE?");
		expectWrite("VOL10VOL");
		expectWrite("MOFF");
		m_control.replay();
		m_sound.setCodes(config);
		m_sound.setWriter(m_writer);
		m_sound.setMuteStatus(true);
		m_sound.setVolume(10);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetVolumeStatus() throws IOException
	{
		MapConfiguration config = getConfig();

		expectWrite("?VOL?");
		expectWrite("?MUTE?");

		m_control.replay();
		m_sound.setCodes(config);
		m_sound.setWriter(m_writer);
		m_sound.setVolumeStatus(5);
		assertEquals(5, m_sound.getVolume());
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testMute() throws IOException
	{
		MapConfiguration config = getConfig();

		expectWrite("?VOL?");
		expectWrite("?MUTE?");
		expectWrite("MOFF");

		m_control.replay();
		m_sound.setCodes(config);
		m_sound.setWriter(m_writer);
		m_sound.mute(false);
		m_control.verify();
		m_control.reset();

		expectWrite("MON");
		m_control.replay();
		m_sound.mute(true);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetMuteStatus() throws IOException
	{
		MapConfiguration config = getConfig();

		expectWrite("?VOL?");
		expectWrite("?MUTE?");

		m_control.replay();
		m_sound.setCodes(config);
		m_sound.setWriter(m_writer);
		m_sound.setMuteStatus(false);
		assertEquals(false, m_sound.isMute());
		m_control.verify();
		m_control.reset();

		m_control.replay();
		m_sound.setMuteStatus(true);
		assertEquals(true, m_sound.isMute());
		m_control.verify();
		m_control.reset();
	}

	private MapConfiguration getConfig()
	{
		MapConfiguration config = new MapConfiguration(new HashMap<String, Object>());
		config.getMap().put("volume.Set", "VOL%1$sVOL");
		config.getMap().put("volume.Query", "?VOL?");
		config.getMap().put("volume.Max", "100");
		config.getMap().put("mute.On", "MON");
		config.getMap().put("mute.Off", "MOFF");
		config.getMap().put("mute.Query", "?MUTE?");
		return config;
	}

	private void expectWrite(String command) throws IOException
	{
		EasyMock.expect(m_writer.write(ByteBuffer.wrap(command.getBytes()))).andReturn(command.length());
	}
}
