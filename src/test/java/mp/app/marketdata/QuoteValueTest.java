package mp.app.marketdata;

import static mp.app.TestUtils.EPSILON;

import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

public class QuoteValueTest extends TestCase {

	public void testParsingHappyFlow() throws Exception {
		Collection<String> col = Arrays.asList(
			"TEST.TEST	 123.4",
			"TEST.TEST 123.4		",
			"TEST.TEST   123.40000");
		
		for (String str : col) {
			QuoteValue qv = QuoteValue.parse(str);
			assertEquals("TEST.TEST", qv.getName());
			assertEquals(123.4, qv.getPrice(), EPSILON);
		}
	}

	public void testParsingUnparsableVal() throws Exception {
		Collection<String> col = Arrays.asList(
			"TEST.TEST	 garbage",
			"TEST.TEST",
			"TEST.TEST 123sd");
		
		for (String str : col) {
			QuoteValue qv = QuoteValue.parse(str);
			assertEquals("TEST.TEST", qv.getName());
			assertTrue(Double.isNaN(qv.getPrice()));
		}
	}
}
