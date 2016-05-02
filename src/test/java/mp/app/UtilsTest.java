package mp.app;

import static mp.app.TestUtils.EPSILON;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Set;

import junit.framework.TestCase;
import mp.app.marketdata.MarketData;

public class UtilsTest extends TestCase {

	static final String TEST_FEED = "test.feed";

	static final String TEST_BASKET = "test.basket";

	public UtilsTest(String testName) {
		super(testName);
	}
	
	public void testBasketLoad() throws Exception {
		URL url = getClass().getClassLoader().getResource(TEST_BASKET);
		Set<Asset> basket = Utils.getBasket(Paths.get(url.toURI()));
		assertNotNull(basket);
		assertEquals(5, basket.size());
	}

	public void testMarketDataLoad() throws Exception {
		URL url = getClass().getClassLoader().getResource(TEST_FEED);
		MarketData md = Utils.getMarketDataWithLock(Paths.get(url.toURI()));
		assertNotNull(md);
		assertEquals(6, md.getQuotes().size());
		
		assertEquals(3.5, md.getQuote("FRUIT.BANA").getPrice(), EPSILON);
		assertEquals(2.99, md.getQuote("FRUIT.ORAN").getPrice(), EPSILON);
		assertEquals(92.77, md.getQuote("FRUIT.AAPL").getPrice(), EPSILON);
		assertEquals(1.59, md.getQuote("FRUIT.LEMO").getPrice(), EPSILON);
		assertEquals(2.72, md.getQuote("FRUIT.PEAC").getPrice(), EPSILON);

		assertTrue(Double.isNaN(md.getQuote("nonexistent").getPrice()));
	}
}