package mp.app;

import static mp.app.TestUtils.EPSILON;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

public class AssetTest extends TestCase {

	public AssetTest(String testName) {
		super(testName);
	}

	public void testParsingHappyFlow() throws Exception {
		Collection<String> col = Arrays.asList(
			"FRUIT.BANA	11.001	Bananas",
			"FRUIT.BANA		11.001		Bananas	will not be used",
			"FRUIT.BANA	11.00100	Bananas"
		);
		
		for (String str : col) {
			Asset a = Asset.parse(str);
			assertEquals("FRUIT.BANA", a.getQuoteName());
			assertEquals("Bananas", a.getName());
			assertEquals(11.001, a.getQuantity(), EPSILON);
		}
	}
	
	public void testParsingErrors() {
		Collection<String> col = Arrays.asList(
			"FRUIT.BANA",
			"FRUIT.BANA	11.001blah	Bananas",
			"FRUIT.BANA	11.001"
		);
		
		for (String str : col) {
			try {
				Asset.parse(str);
				fail("Fail expected for string: " + str);
			} catch (Exception e) {
				// OK
			}
		}
	}
}