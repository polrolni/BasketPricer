package mp.app;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import mp.app.marketdata.MarketData;
import mp.app.marketdata.QuoteValue;

/**
 * Utility methods 
 */
public class Utils {
	
	static final NumberFormat FORMAT;
	
	static {
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		dfs.setGroupingSeparator(',');
		dfs.setNaN("NaN");
		FORMAT = new DecimalFormat("0.00000", dfs);
	}
	
	/**
	 * Imports the market data representation from a file to object representation.
	 * Note that the implementation is explicitly ignoring any errors which may occur
	 * during parsing the data and ensures that in worst case an empty market data object
	 * will be returned.
	 * 
	 * Notice the logic uses nio file channel to  
	 * 
	 * @param path	path to the file representation
	 * @return	an instance of market data 
	 */
	public static MarketData getMarketDataWithLock(Path path) {
		Map<String, QuoteValue> quotes = new HashMap<>();
		
		try (
			FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
			BufferedReader in = new BufferedReader(
				new InputStreamReader(
					Channels.newInputStream(channel)));
		) {
			/*
			 * Locking the channel, unlock is not explicitly done in code,
			 * it will be automatically applied when closing the channel.
			 */
			channel.lock(); 
			
			Map<String, QuoteValue> map = in.lines()
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.filter(s -> !s.startsWith("#"))
				.map(s -> { 
					try {
						return QuoteValue.parse(s);
					} catch (Exception e) {
						System.err.println(e);
						return null;
					}
				})
				.filter(Objects::nonNull)
 				.collect(toMap(q -> q.getName(), Function.identity()));
			quotes.putAll(map);
		} catch (Exception e) {
			System.err.println(e);
		}
		
		return new MarketData() {		
			@Override
			public QuoteValue getQuote(String name) {
				return quotes.get(name);
			}

			@Override
			public Collection<QuoteValue> getQuotes() {
				return quotes.values();
			}
		};
	}
	
	/**
	 * Persists market data to a feed file
	 * 
	 * @param path	destination file
	 * @param quotes	market data (quotes)
	 * @param origin	name of saving agent
	 * @throws IOException
	 */
	public static void persistQuoteValuesWithLock(Path path, Map<String, Double> quotes, String origin) throws IOException {
		try (
			FileOutputStream fos = new FileOutputStream(path.toFile(), false);
			PrintStream out = new PrintStream(fos);
			FileChannel channel = fos.getChannel();
		) {
			/*
			 * Locking the channel, unlock is not explicitly done in code,
			 * it will be automatically applied when closing the channel.
			 */			
			channel.lock();

			Stream.of(
					"################################	"
				,	"# Market Data File					"
				, 	"# Origin: " + origin
				,	"# Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
				,	"#									"	
				,	"# Syntax: quote_name quote_value	"
				, 	"################################ 	"
			).forEach(out::println);

			out.println();
			
			quotes.entrySet().stream()
				.map(Utils::renderLine)
				.forEach(out::println);
		}
	}	
	
	/**
	 * Retrieves basket definition from a file
	 * 
	 * @param path	path to basket file
	 * @return	basket's content or empty set when error occurs
	 */
	public static Set<Asset> getBasket(Path path) {
		try (Stream<String> stream = Files.lines(path)) {
			return stream
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.filter(s -> !s.startsWith("#"))
				.map(s -> { 
					try {
						return Asset.parse(s);
					} catch (Exception e) {
						System.err.println(e);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(toCollection(HashSet::new));
		} catch (Exception e) {
			System.err.println(e);
			return Collections.emptySet();
		}
	}
	
	static String renderLine(Map.Entry<String, Double> e) {
		String str = String.format("%-10s %s", e.getKey(), FORMAT.format(e.getValue()));		
		return str;
	}
}
