package mp.app;

import java.util.Collection;

import mp.app.marketdata.MarketData;
import mp.app.marketdata.QuoteValue;

public class TestUtils {
	
	public static final double EPSILON;
		
	static {
		double e = 1.0;
		while (1.0 + 0.5 * e != 1.0) {
			e *= 0.5;
		}
		EPSILON = e;
	}
	
	public static MarketData of(Collection<QuoteValue> col) {
		return new MarketData() {
			
			@Override
			public Collection<QuoteValue> getQuotes() {
				return col;
			}
			
			@Override
			public QuoteValue getQuote(String name) {
				return col.stream()
					.filter(q -> name.equals(q.getName()))
					.findAny()
					.orElse(null);
			}
		};
	}
}
