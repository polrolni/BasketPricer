package mp.app;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

public class Utils {
	
	public static MarketData getMarketDataWithLock(Path path) {
		Map<String, QuoteValue> quotes = new HashMap<>();
		
		try (
			FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
			BufferedReader in = new BufferedReader(
				new InputStreamReader(
					Channels.newInputStream(channel)));
		) {
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
}
