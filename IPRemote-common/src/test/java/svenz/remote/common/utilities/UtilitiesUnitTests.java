/**
 * 
 */
package svenz.remote.common.utilities;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import java.util.Collections;
import org.junit.Test;

/**
 * @author Sven Zethelius
 *
 */
public class UtilitiesUnitTests
{
	@Test
	public void testMergeOrderedList() throws Exception
	{
		assertEquals(Collections.<String> emptyList(),
				Utilities.mergeOrderedLists(Collections.<String> emptyList(), asList("1", "2", "3")));

		assertEquals(asList("1", "2", "3"),
				Utilities.mergeOrderedLists(asList("2", "1", "3"), asList("1", "2", "3")));

		assertEquals(asList("2", "3"),
				Utilities.mergeOrderedLists(asList("2", "3"), asList("1", "2", "3")));

		assertEquals(asList("1", "3"),
				Utilities.mergeOrderedLists(asList("1", "3"), asList("1", "2", "3")));

		assertEquals(asList("1", "2", "3"),
				Utilities.mergeOrderedLists(asList("1", "2","3"), asList("1", "2")));

		assertEquals(asList("1", "2", "3"),
				Utilities.mergeOrderedLists(asList("1", "2", "3"), Collections.<String> emptyList()));

		assertEquals(asList("1", "4"),
				Utilities.mergeOrderedLists(asList("1", "4"), asList("1", "2", "3")));


	}
}
