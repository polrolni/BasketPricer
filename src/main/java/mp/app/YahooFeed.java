package mp.app;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Exemplary request url: 
 * http://finance.yahoo.com/d/quotes.csv?s=GOOGL,nonexistent,AAPL,MSFT,FB&f=l1
 * 
 * Exemplary answer:
 * 707.88
 * N/A
 * 93.74
 * 49.87
 * 117.58
 */
public class YahooFeed extends MarketDataFeed {
	
	static String YAHOO_URL = "http://finance.yahoo.com/d/quotes.csv";
	
	static String YAHOO_DATA = "l1"; // last price, refer to 
	
	public static void main(String[] args) throws Exception {
		new YahooFeed().execute(args);
	}
	
	@Override
	protected Map<String, Double> fetch(Set<String> names) throws Exception {
		Set<String> orderedNames = new TreeSet<>(names);

		String s = YAHOO_URL 
			+ '?' + "s=" + orderedNames.stream().collect(joining(",")) 
			+ '&' + "f=" + YAHOO_DATA;
		URL url = new URL(s);
		
		try (BufferedReader in = new BufferedReader(
			new InputStreamReader(
				url.openStream()))) 
		{
			Iterator<String> it = orderedNames.iterator();		
			Map<String, Double> ret = in.lines()
				.filter(d -> it.hasNext())
				.map(String::trim)
				.map(q -> "N/A".equals(q) 
					? Double.NaN 
					: Double.valueOf(q))
				.collect(toMap(k -> it.next(), identity()));
			return ret;
		}
	}
}
