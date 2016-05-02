package mp.app.marketdata;

import java.util.Collection;

public interface  MarketData {
	
	QuoteValue getQuote(String name);
	
	Collection<QuoteValue> getQuotes();
}
	