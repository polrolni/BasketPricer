package mp.app.marketdata;

import java.util.Collection;

/**
 * Represents a set of all possible market data 
 * to be used in measures calculations
 * 
 * Presently only quotes are supported
 */
public interface  MarketData {
	
	/**
	 * Retrieves a quote 
	 * 
	 * @param name	quote name
	 * @return	quote value if found,
	 * 			null otherwise
	 */
	QuoteValue getQuote(String name);
	
	/**
	 * @return all available quote values
	 */
	Collection<QuoteValue> getQuotes();
}
	