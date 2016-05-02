package mp.app;

import static mp.app.TestUtils.EPSILON;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import junit.framework.TestCase;
import mp.app.marketdata.QuoteValue;
import mp.app.measure.Measure;
import mp.app.measure.PriceMeasure;

public class BasketPricerTest extends TestCase {

	static final Measure PRICER_MEASURE = new PriceMeasure();
	
	static final String TEST_FEED = "test.feed";

	static final String TEST_BASKET = "test.basket";
	
	static final double[][] TEST_QTY_PRICE= {
		{ 11.001,	3.5 },
		{ 10.0, 	2.99 },
		{ 5.0, 		92.77 },
		{ 0.5, 		1.59 },
		{ 2.7, 		2.72 }
	};

	public BasketPricerTest(String testName) {
		super(testName);
	}

	public void testSimpleTotals() throws Exception {
		URL url = getClass().getClassLoader().getResource(TEST_BASKET);
		Path basket = Paths.get(url.toURI());		
		url = getClass().getClassLoader().getResource(TEST_FEED);
		Path md = Paths.get(url.toURI());
		
		double totals = new BasketPricer().valuate(
			basket, 
			md, 
			new PrintStream(new OutputStream() {
				public void write(int b) throws IOException {}
			}));
		
		double expected = Arrays.asList(TEST_QTY_PRICE)
			.stream()
			.mapToDouble(d -> d[0] * d[1])
			.sum();
		
		assertEquals(expected, totals, EPSILON);
	}

	public void testEmptyBasket() {
		Map<Asset, Double> result = new BasketPricer().valuate(
			Collections.emptySet(), 
			TestUtils.of(Collections.emptyList()), 
			PRICER_MEASURE);
		
		assertTrue(result.isEmpty());		
	}

	public void testEmptyMarketData() throws Exception {
		BasketPricer p = new BasketPricer();
		URL url = getClass().getClassLoader().getResource(TEST_BASKET);
		Set<Asset> basket = Utils.getBasket(Paths.get(url.toURI()));

		Map<Asset, Double> result = p.valuate(
			basket, 
			TestUtils.of(Collections.emptyList()), 
			PRICER_MEASURE);

		assertEquals(basket.size(), result.size());
		
		boolean allNan = result.values().stream()
			.mapToDouble(Double::doubleValue)
			.allMatch(Double::isNaN);
		assertTrue(allNan);
	}

	public void testSaturateCPUCores() {
		Measure m = (a, b) -> {
			try { Thread.sleep(100); } catch (Exception e) {}
			return 1d; 
		};
		
		int size = Runtime.getRuntime().availableProcessors() * 10;
		
		Set<Asset> basket = IntStream
			.range(0, size)
			.mapToObj(i -> new Asset("Asset " + i, "ASSET." + i, 1))
			.collect(Collectors.toCollection(HashSet::new));
		
		Collection<QuoteValue> quotes = IntStream
			.range(0, size)
			.mapToObj(i -> new QuoteValue("ASSET." + i, 1))
			.collect(Collectors.toList());

		Map<Asset, Double> results = new BasketPricer().valuate(
			basket, 
			TestUtils.of(quotes), 
			m);
		
		double totals = results.values().stream().mapToDouble(Double::doubleValue).sum();
		
		assertEquals(size, results.size());
		assertEquals(size, totals, EPSILON);
	}
}
