/**
 * 
 */
package svenz.remote.device.ipremote;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.log4j.Level;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Test;
import svenz.remote.common.utilities.SimplePropertiesConfiguration;
import svenz.remote.device.IChangable.IChangeListener;
import svenz.remote.net.nio.TCPSocketChannelInstance;
import svenz.test.helper.CaptureAppender;

/**
 * @author Sven Zethelius
 *
 */
public class AbstractCodedUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final AbstractCoded<String> m_coded = new AbstractCoded<String>("Test", null);

	@After
	public void teardown()
	{
		CaptureAppender.reset();
	}

	@Test
	public void testConstructWithTarget() throws Exception
	{
		AbstractCoded<String> coded = new AbstractCoded<String>("Test", this);

		IChangeListener changeListener = m_control.createMock("ChangeListener", IChangeListener.class);
		changeListener.stateChanged(this, "Test");

		m_control.replay();
		coded.addChangeListener(changeListener);
		coded.setStatus("A");
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetWriter() throws Exception
	{
		WritableByteChannel channel = m_control.createMock("Channel", WritableByteChannel.class);

		m_control.replay();
		m_coded.setStatus("A");
		m_coded.setWriter(channel);
		assertEquals("A", m_coded.getStatus());
		m_control.verify();
		m_control.reset();

		m_control.replay();
		m_coded.setWriter(null);
		assertEquals(null, m_coded.getStatus());
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetWriterWithQuery() throws Exception
	{
		WritableByteChannel channel = m_control.createMock("Channel", WritableByteChannel.class);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Query", "SendQuery");
		expectWrite(channel, "SendQuery");

		m_control.replay();
		m_coded.setCodes(new MapConfiguration(map));
		m_coded.setStatus("A");
		m_coded.setWriter(channel);
		assertEquals("A", m_coded.getStatus());
		m_control.verify();
		m_control.reset();

		m_control.replay();
		m_coded.setWriter(null);
		assertEquals(null, m_coded.getStatus());
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testStatus() throws Exception
	{
		IChangeListener changeListener = m_control.createMock("ChangeListener", IChangeListener.class);
		changeListener.stateChanged(m_coded, "Test");
		changeListener.stateChanged(m_coded, "Test");
		changeListener.stateChanged(m_coded, "Test");

		m_control.replay();
		m_coded.addChangeListener(changeListener);
		m_coded.setStatus("A");
		assertEquals("A", m_coded.getStatus());
		m_coded.setStatus("A");
		assertEquals("A", m_coded.getStatus());
		m_coded.setStatus(new String("A"));
		assertEquals("A", m_coded.getStatus());
		m_coded.setStatus("B");
		assertEquals("B", m_coded.getStatus());
		m_coded.setStatus(null);
		assertEquals(null, m_coded.getStatus());
		m_coded.removeChangeListener(changeListener);
		m_coded.setStatus("C");
		assertEquals("C", m_coded.getStatus());
		m_coded.setStatus(null);
		assertEquals(null, m_coded.getStatus());
		m_coded.setStatus(null);
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetDeviceListener() throws Exception
	{
		ChannelDeviceListener listener = m_control.createMock("ChannelDeviceListener", ChannelDeviceListener.class);
		Capture<IChangeListener> captureListeners = new Capture<IChangeListener>(CaptureType.ALL);
		listener.addChangeListener(EasyMock.capture(captureListeners));

		m_control.replay();
		m_coded.setDeviceListener(listener);
		m_control.verify();
		m_control.reset();

		TCPSocketChannelInstance instance = m_control.createMock("Instance", TCPSocketChannelInstance.class);
		EasyMock.expect(listener.getInstance()).andReturn(instance);
		m_control.replay();
		captureListeners.getValue().stateChanged(m_coded, "Test");
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testQuery() throws Exception
	{
		WritableByteChannel channel = m_control.createMock("Channel", WritableByteChannel.class);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Query", "SendQuery");
		expectWrite(channel, "SendQuery");
		expectWrite(channel, "SendQuery");

		m_control.replay();
		m_coded.setCodes(new MapConfiguration(map));
		m_coded.setWriter(channel);
		m_coded.query();
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetCodes() throws Exception
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Query", "SendQuery");
		map.put("On", "SendOn");
		map.put("Off", "SendOn,SendOff");

		m_control.replay();
		MapConfiguration config = new MapConfiguration(map);
		config.setDelimiterParsingDisabled(true);
		m_coded.setCodes(config);
		assertEquals(Arrays.asList("SendQuery"), m_coded.getCodes().get("Query"));
		assertEquals(Arrays.asList("SendOn"), m_coded.getCodes().get("On"));
		assertEquals(Arrays.asList("SendOn", "SendOff"), m_coded.getCodes().get("Off"));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testGetCommand() throws Exception
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Query", "SendQuery");
		map.put("On", "SendOn");
		map.put("Off", "SendOff");

		m_control.replay();
		MapConfiguration config = new MapConfiguration(map);
		config.setDelimiterParsingDisabled(true);
		m_coded.setCodes(config);
		assertEquals("Query", m_coded.getCommand("SendQuery"));
		assertEquals("On", m_coded.getCommand("SendOn"));
		assertEquals("Off", m_coded.getCommand("SendOff"));
		assertEquals(null, m_coded.getCommand("Unknown"));
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalStateException.class)
	public void testGetCommandMultipleCommands() throws Exception
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Query", "SendQuery");
		map.put("On", "SendOn");
		map.put("Off", "Send2,SendOff");

		m_control.replay();
		MapConfiguration config = new MapConfiguration(map);
		config.setDelimiterParsingDisabled(true);
		m_coded.setCodes(config);
		assertEquals("Query", m_coded.getCommand("SendQuery"));
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalStateException.class)
	public void testGetCommandOverlapping() throws Exception
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Query", "SendQuery");
		map.put("On", "SendOn");
		map.put("Off", "SendOn");

		m_control.replay();
		MapConfiguration config = new MapConfiguration(map);
		config.setDelimiterParsingDisabled(true);
		m_coded.setCodes(config);
		assertEquals("Query", m_coded.getCommand("SendQuery"));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testFire() throws Exception
	{
		WritableByteChannel channel = m_control.createMock("Channel", WritableByteChannel.class);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("On", "SendOn");
		expectWrite(channel, "SendOn");

		m_control.replay();
		m_coded.setCodes(new MapConfiguration(map));
		m_coded.setWriter(channel);
		m_coded.fire("On");
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFireNoCodes() throws Exception
	{
		WritableByteChannel channel = m_control.createMock("Channel", WritableByteChannel.class);

		m_control.replay();
		m_coded.setWriter(channel);
		m_coded.fire("On");
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testFireCustom() throws Exception
	{
		WritableByteChannel channel = m_control.createMock("Channel", WritableByteChannel.class);

		expectWrite(channel, "A");
		expectWrite(channel, "B");

		m_control.replay();
		m_coded.setWriter(channel);
		m_coded.fire("On", Arrays.asList("A", "B"));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testFireFormatted() throws Exception
	{
		WritableByteChannel channel = m_control.createMock("Channel", WritableByteChannel.class);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("On", "SendOn%2$s%1$s");
		expectWrite(channel, "SendOnCDAB");

		m_control.replay();
		m_coded.setCodes(new MapConfiguration(map));
		m_coded.setWriter(channel);
		m_coded.fireFormatted("On", "AB", "CD");
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFireFormattedNoCodes() throws Exception
	{
		WritableByteChannel channel = m_control.createMock("Channel", WritableByteChannel.class);

		m_control.replay();
		m_coded.setWriter(channel);
		m_coded.fireFormatted("On", "AB", "CD");
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFireFormatteMultiple() throws Exception
	{
		WritableByteChannel channel = m_control.createMock("Channel", WritableByteChannel.class);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("On", "SendOn%2$s%1$s,Foo");

		m_control.replay();
		MapConfiguration config = new MapConfiguration(map);
		config.setDelimiterParsingDisabled(true);
		m_coded.setCodes(config);

		m_coded.setWriter(channel);
		m_coded.fireFormatted("On", "AB", "CD");
		m_control.verify();
		m_control.reset();
	}
	@Test
	public void testFireException() throws Exception
	{
		CaptureAppender appender = CaptureAppender.install(AbstractCoded.class.getName());
		appender.setThreshold(Level.ERROR);
		WritableByteChannel channel = m_control.createMock("Channel", WritableByteChannel.class);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("On", "SendOn");

		EasyMock.expect(channel.write(ByteBuffer.wrap("SendOn".getBytes())))
				.andThrow(new IOException("Test Exception"));

		m_control.replay();
		m_coded.setCodes(new MapConfiguration(map));
		m_coded.setWriter(channel);

		m_coded.fire("On");
		m_control.verify();
		m_control.reset();
		assertEquals(1, appender.getEvents().size());
	}

	@Test
	public void testFireNoWriter() throws Exception
	{
		CaptureAppender appender = CaptureAppender.install(AbstractCoded.class.getName());
		appender.setThreshold(Level.ERROR);

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("On", "SendOn");

		m_control.replay();
		m_coded.setCodes(new MapConfiguration(map));
		m_coded.fire("On");
		m_control.verify();
		m_control.reset();
		assertEquals(1, appender.getEvents().size());
	}

	@Test
	public void testFilterCodes() throws Exception
	{
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Query", "SendQuery");
		map.put("On", "SendOn");
		map.put("Off", "SendOff");

		MapConfiguration config = new MapConfiguration(map);
		config.setDelimiterParsingDisabled(true);

		m_control.replay();
		Configuration filterCodes = m_coded.filterCodes(config, "Query", "On", "Foo");
		m_control.verify();
		m_control.reset();
		
		assertEquals(new HashSet<String>(Arrays.asList("Query","On")), 
				((SimplePropertiesConfiguration)filterCodes).getStore().keySet());
	}


	private void expectWrite(WritableByteChannel channel, String s) throws IOException
	{
		EasyMock.expect(channel.write(ByteBuffer.wrap(s.getBytes()))).andReturn(s.length());
	}
}
