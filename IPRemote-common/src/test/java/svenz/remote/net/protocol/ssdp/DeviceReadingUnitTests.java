/**
 * 
 */
package svenz.remote.net.protocol.ssdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import org.simpleframework.xml.core.Persister;
import svenz.remote.net.protocol.ssdp.jaxb.Device;
import svenz.remote.net.protocol.ssdp.jaxb.Root;
import svenz.remote.net.protocol.ssdp.jaxb.Service;

/**
 * @author Sven Zethelius
 *
 */
public class DeviceReadingUnitTests
{
	@Test
	public void testRoot() throws Exception
	{
		Persister persister = new Persister();

		InputStream is = getClass().getResourceAsStream("device.xml");
		assertNotNull(is);
		Root root = persister.read(Root.class, is);
		assertNotNull(root);
		Device device = root.getDevice();
		assertNotNull(device);
		assertEquals("urn:pioneer-co-jp:device:PioControlServer:1", device.getDeviceType());
		assertEquals("Test device", device.getFriendlyName());
		assertEquals("PIONEER CORPORATION", device.getManufacturer());
		assertEquals("BDP-150/UCXESM", device.getModelName());
		assertEquals("uuid:5f9ec1b3-ed59-79bc-4530-745e1c1c832c", device.getUDN());
		assertEquals((Integer) 8102, device.getIPPort());

		List<Service> services = device.getServices();
		assertNotNull(services);
		assertEquals(3, services.size());

	}

}
