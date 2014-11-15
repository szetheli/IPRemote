/**
 * 
 */
package svenz.remote.devicetests.device.impl.pioneer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svenz.remote.device.IChangable.IChangeListener;
import svenz.remote.device.impl.pioneer.PlayerBDP150Device;
import svenz.remote.net.nio.SocketChannelManager;
import svenz.remote.net.protocol.ssdp.SSDPManager;

/**
 * @author Sven Zethelius
 *
 */
public class PlayerBDP150DeviceTests
{
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerBDP150DeviceTests.class);

	@Test
	public void testDevice() throws Exception
	{
		ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);
		SocketChannelManager channelManager = new SocketChannelManager();
		SSDPManager ssdpManager = new SSDPManager(channelManager, executor);
		PlayerBDP150Device device = new PlayerBDP150Device();
		device.setChannelManager(channelManager);
		device.setSSDPManager(ssdpManager);
		device.setExecutor(executor);

		Semaphore semaphorePower = new Semaphore(0), semaphorePlay = new Semaphore(0);
		device.getPowered().addChangeListener(new SemaphoreListener(semaphorePower));
		device.getPlayable().addChangeListener(new SemaphoreListener(semaphorePlay));

		channelManager.open();
		ssdpManager.open();

		// figure out initial state
		await(semaphorePower);
		assertTrue(device.isConnected());

		// ensure we are in a powered off state to start the test
		if(device.getPowered().isPowered())
		{
			LOGGER.info("Powering off Player");
			device.getPowered().powerOff();
			await(semaphorePower);
			assertEquals(false, device.getPowered().isPowered());
			semaphorePlay.drainPermits(); // reset any play notifications
		}

		device.getPowered().powerOn();
		await(semaphorePower);
		assertEquals(true, device.getPowered().isPowered());

		await(semaphorePlay);
		Thread.sleep(1000);

		device.close();
		ssdpManager.close();
		channelManager.close();
	}
	
	private void await(Semaphore s) throws InterruptedException
	{
		assertEquals(true, s.tryAcquire(1, TimeUnit.MINUTES));
	}

	private static class SemaphoreListener implements IChangeListener
	{
		private final Semaphore m_semaphore;

		public SemaphoreListener(Semaphore s)
		{
			m_semaphore = s;
		}
		
		@Override
		public void stateChanged(Object target, String property)
		{
			m_semaphore.release();
		}
		
	}
}
