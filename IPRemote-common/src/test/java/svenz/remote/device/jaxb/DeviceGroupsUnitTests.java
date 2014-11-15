/**
 * 
 */
package svenz.remote.device.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.InputStream;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import svenz.remote.device.impl.sharp.TVAquas60LE650Device;

/**
 * @author Sven Zethelius
 *
 */
public class DeviceGroupsUnitTests
{
	private JAXBContext m_context;

	@Before
	public void setup() throws JAXBException
	{
		m_context = JAXBContext.newInstance(DeviceGroups.class);
	}

	@Test
	@Ignore
	public void testRead() throws Exception
	{
		Unmarshaller unmarshaller = m_context.createUnmarshaller();
		InputStream stream = getClass().getResourceAsStream(getClass().getSimpleName() + ".xml");
		assertNotNull(stream);
		DeviceGroups deviceGroups = (DeviceGroups) unmarshaller.unmarshal(stream);
		validate(deviceGroups);
	}

	@Test
	public void testReadSimpleXML() throws Exception
	{
		Serializer serializer = new Persister();
		InputStream stream = getClass().getResourceAsStream(getClass().getSimpleName() + ".xml");
		assertNotNull(stream);

		DeviceGroups deviceGroups = serializer.read(DeviceGroups.class, stream);
		validate(deviceGroups);
	}

	private void validate(DeviceGroups deviceGroups) throws Exception
	{
		assertEquals(3, deviceGroups.getDevices().size());
		assertEquals("TV", deviceGroups.getDevices().get(0).getName());
		assertEquals(1, deviceGroups.getGroups().size());

		Device device = deviceGroups.getDevices().get(0);
		assertEquals("TV", device.getName());
		Object o = device.getImplInstance();
		assertTrue(o instanceof TVAquas60LE650Device);

		Group group = deviceGroups.getGroups().get(0);
		assertEquals("zone.1", group.getLocation());
		assertEquals("dvd", group.getActivity());

		validate(group.getPlayable(), "BD.playable");
		validate(group.getSound(), "Receiver.zone1.sound");
		List<MenuReference> menus = group.getMenuReferences();
		assertEquals(3, menus.size());
		MenuReference menu = menus.get(0);
		validate(menu, "BD.menuDisk");
		assertEquals("disk", menu.getName());

		List<SelectableReference> selects = group.getSelectableReferences();
		assertEquals(2, selects.size());
		SelectableReference select = selects.get(0);
		validate(select, "TV.input");
		assertEquals("hdmi.1", select.getValue());
	}

	private void validate(DeviceReference reference, String ref)
	{
		assertNotNull(reference);
		assertEquals(ref, reference.getReference());

	}
}
