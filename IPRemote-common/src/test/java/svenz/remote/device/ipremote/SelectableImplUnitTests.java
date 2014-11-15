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
import org.apache.commons.configuration.MapConfiguration;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Sven Zethelius
 *
 */
public class SelectableImplUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final SelectableImpl m_selectable = new SelectableImpl();
	private final WritableByteChannel m_channel = m_control.createMock("Channel", WritableByteChannel.class);

	@Before
	public void setUp() throws Exception
	{
	}

	@Test
	public void testSetSelectionDirectList() throws IOException
	{
		MapConfiguration config = new MapConfiguration(new HashMap<String, Object>());
		config.getMap().put("in1", "out1");
		config.getMap().put("in2", "out2");
		config.getMap().put("in3", "out3");

		expectWrite("out1");
		m_control.replay();
		m_selectable.setCodes(config);
		m_selectable.setWriter(m_channel);
		m_selectable.setSelection("in1");
		assertEquals(new HashSet<String>(Arrays.asList("in1", "in2", "in3")),
				new HashSet<String>(m_selectable.getOptions()));
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetSelectionFormatted() throws Exception
	{
		MapConfiguration config = new MapConfiguration(new HashMap<String, Object>());
		config.getMap().put("Set", "Set%1$s");
		config.getMap().put("Query", "?Select?");

		expectWrite("?Select?");
		expectWrite("Setin1");
		m_control.replay();
		m_selectable.setCodes(config);
		m_selectable.setOptions(Arrays.asList("in1", "in2", "in3"));
		m_selectable.setWriter(m_channel);
		m_selectable.setSelection("in1");
		assertEquals(new HashSet<String>(Arrays.asList("in1", "in2", "in3")),
				new HashSet<String>(m_selectable.getOptions()));
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetSelectionInvalid() throws Exception
	{
		MapConfiguration config = new MapConfiguration(new HashMap<String, Object>());
		config.getMap().put("in1", "out1");
		config.getMap().put("in2", "out2");
		config.getMap().put("in3", "out3");

		m_control.replay();
		m_selectable.setCodes(config);
		m_selectable.setWriter(m_channel);
		m_selectable.setSelection("invalid");
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetStatus()
	{
		MapConfiguration config = new MapConfiguration(new HashMap<String, Object>());
		config.getMap().put("in1", "out1");
		config.getMap().put("in2", "out2");
		config.getMap().put("in3", "out3");

		m_control.replay();
		m_selectable.setCodes(config);
		m_selectable.setWriter(m_channel);
		m_selectable.setStatus("in1");
		assertEquals("in1", m_selectable.getSelection());
		m_control.verify();
		m_control.reset();
	}

	@Test
	public void testSetStatusNull()
	{
		MapConfiguration config = new MapConfiguration(new HashMap<String, Object>());
		config.getMap().put("in1", "out1");
		config.getMap().put("in2", "out2");
		config.getMap().put("in3", "out3");

		m_control.replay();
		m_selectable.setCodes(config);
		m_selectable.setWriter(m_channel);
		m_selectable.setStatus(null);
		assertEquals(null, m_selectable.getSelection());
		m_control.verify();
		m_control.reset();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetStatusInvalid() throws Exception
	{
		MapConfiguration config = new MapConfiguration(new HashMap<String, Object>());
		config.getMap().put("in1", "out1");
		config.getMap().put("in2", "out2");
		config.getMap().put("in3", "out3");

		m_control.replay();
		m_selectable.setCodes(config);
		m_selectable.setWriter(m_channel);
		m_selectable.setStatus("invalid");
		m_control.verify();
		m_control.reset();
	}

	private void expectWrite(String s) throws IOException
	{
		EasyMock.expect(m_channel.write(ByteBuffer.wrap(s.getBytes()))).andReturn(s.length());
	}

}
