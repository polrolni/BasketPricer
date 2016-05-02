package mp.app.measure;

import static mp.app.TestUtils.EPSILON;
import static mp.app.TestUtils.of;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;
import mp.app.Asset;
import mp.app.marketdata.MarketData;
import mp.app.marketdata.QuoteValue;

public class PriceMeasureTest extends TestCase {
	
	String quote = "test.quote";
	Asset a = new Asset("Test", quote, 10);
	
	public void testHappyFlow() {
		QuoteValue qv = new QuoteValue(quote, 2);	
		MarketData md = of(Arrays.asList(qv));
		double val = new PriceMeasure().calculate(a, md);	
		assertEquals(10 * 2d, val, EPSILON);
	}

	public void testNoQuote() {
		MarketData md = of(Collections.emptyList());		
		double val = new PriceMeasure().calculate(a, md);	
		assertTrue(Double.isNaN(val));
	}

	public void testQuoteInvalid() {
		QuoteValue qv = new QuoteValue(quote, Double.NaN);	
		MarketData md = of(Arrays.asList(qv));
		double val = new PriceMeasure().calculate(a, md);	
		assertTrue(Double.isNaN(val));
	}
}
