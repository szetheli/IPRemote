/**
 * 
 */
package svenz.remote.device.ipremote;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Test;
import svenz.remote.device.IPowered;

/**
 * @author Sven Zethelius
 *
 */
public class QueryChangeListenerUnitTests
{
	private final IMocksControl m_control = EasyMock.createControl();
	private final IPowered m_powered = m_control.createMock("Powered", IPowered.class);
	@SuppressWarnings("unchecked")
	private final AbstractCoded<String> m_coded = m_control.createMock("Coded", AbstractCoded.class);
	private final QueryChangeListener m_listener = new QueryChangeListener(m_coded);

	@Test
	public void testStateChangedPoweredOnNoStatus()
	{
		EasyMock.expect(m_powered.isPowered()).andReturn(true);
		EasyMock.expect(m_coded.getStatus()).andReturn(null);
		m_coded.query();
		doStateChange();
	}


	@Test
	public void testStateChangedPoweredOnWithStatus()
	{
		EasyMock.expect(m_powered.isPowered()).andReturn(true);
		EasyMock.expect(m_coded.getStatus()).andReturn("S");
		doStateChange();
	}

	@Test
	public void testStateChangedPoweredOff()
	{
		EasyMock.expect(m_powered.isPowered()).andReturn(false);
		EasyMock.expect(m_coded.setStatus(null)).andReturn(null);
		doStateChange();
	}

	private void doStateChange()
	{
		m_control.replay();
		m_listener.stateChanged(m_powered, "powered");
		m_control.verify();
		m_control.reset();
	}
}
