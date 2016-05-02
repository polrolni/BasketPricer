package mp.app;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.stream.Collectors.toConcurrentMap;

import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import mp.app.marketdata.MarketData;
import mp.app.measure.Measure;
import mp.app.measure.PriceMeasure;

/**
 * Simple pricing for a basket of goods.
 * 
 * It is an attempt to make use of java SE 8 parallelization 
 * provided by java streams.
 */
public class BasketPricer {

	static final NumberFormat FORMAT = new DecimalFormat("#,###,##0.00");

	static final String PARAM_FOLLOW = "-follow";
	
	static final String FILE_SUFFIX_BASKET = "basket";

	static final String FILE_SUFFIX_MKDATA = "feed";

	static final String USAGE = 
			"Usage: java " + BasketPricer.class.getName() + " [" + PARAM_FOLLOW + "]" + " basket_name" + "\n"
		+	"   or: java " + BasketPricer.class.getName() + " [" + PARAM_FOLLOW + "]" + " basket_file marketdata_file" + "\n"
		+	"\n"		
		+	"Parameters:" + "\n"
		+	"   basket_name      basket definition and market data file will be expected in <current_dir>/<basket_name>." + FILE_SUFFIX_BASKET + "\n"
		+	"                    and <current_dir>/<basket_name>." + FILE_SUFFIX_MKDATA + "\n"
		+	"   basket_file      basket definition file" + "\n"
		+	"   marketdata_file  market data file" + "\n"
		+	"\n"
		+	"Options:" + "\n"
		+	"   -follow          continuous mode, program run infinitely and listens for updates of basket and marketdata files," + "\n"
		+	"                    when update detected, the basket will get revaluated." + "\n"
		;

	public static void main(String[] args) {
		boolean isFollow = false;
		Path basket = null;
		Path mkdata = null;

		List<String> list = new ArrayList<>(Arrays.asList(args));
		isFollow = list.remove(PARAM_FOLLOW);
		
		switch (list.size()) {
			case 1:
				String str = list.get(0);
				basket = Paths.get(str + "." + FILE_SUFFIX_BASKET);
				mkdata = Paths.get(str + "." + FILE_SUFFIX_MKDATA);
				break;
				
			case 2:
				basket = Paths.get(list.get(0));
				mkdata = Paths.get(list.get(1));
				break;
		}
		
		if (basket == null || mkdata == null) {
			System.out.println(USAGE);
		} else {
			BasketPricer bp = new BasketPricer();
			bp.valuate(basket, mkdata, System.out);
			if (isFollow) {
				bp.startService(basket, mkdata, System.out);
			}
		}
	}

	/**
	 * Entry point to basket valuation
	 * 
	 * @param basketDef		path to file containing basket definition
	 * @param marketData	path to file containing market data (quotes)
	 * @param out	output stream
	 */
	public void valuate(Path basketDef, Path marketData, PrintStream out) {
		Set<Asset> basket = Utils.getBasket(basketDef);
		MarketData md = Utils.getMarketDataWithLock(marketData);
		Measure measure = new PriceMeasure();

		/*
		 * display initial information
		 */
		out.println("Valuation date-time:    " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		out.println("Current directory:      " + Paths.get(".").toAbsolutePath().normalize());
		out.println("Basket definition file: " + basketDef);
		out.println("Market data file:       " + marketData);
		out.println();
		
		/*
		 * pricing
		 * 
		 * Parallelization of computations done via parallel stream.
		 * Calculation results will be collected in a map.
		 */
		Map<Asset, Double> map = basket
			.parallelStream()
			.collect(toConcurrentMap(
				Function.identity(),
				c -> measure.calculate(c, md)));

		/*
		 * reporting
		 */
		// list of basket components		
		map.entrySet()
			.stream()
			.sorted((a, b) -> a.getKey().getName().compareTo(b.getKey().getName()))
			.map(this::renderLine)
			.forEach(out::println);

		// bottom line
		DoubleSummaryStatistics stats = map.values()
			.parallelStream()
			.mapToDouble(Double::doubleValue)
			.summaryStatistics();
		out.println("----");
		out.println(renderTotals(stats));
		out.println();
	}
	
	/**
	 * 
	 * @param basketDef
	 * @param marketData
	 * @param out
	 */
	@SuppressWarnings("unchecked")
	public void startService(Path basketDef, Path marketData, PrintStream out) {		
		try (WatchService service = FileSystems.getDefault().newWatchService()) {
			Path bDir = basketDef.toAbsolutePath().getParent();
			bDir.register(service, ENTRY_CREATE, ENTRY_MODIFY);

			out.println("Watcher service set on directory: " + bDir);
			
			Path mdDir = marketData.toAbsolutePath().getParent();
			if (!Files.isSameFile(bDir, mdDir)) {
				mdDir.register(service, ENTRY_CREATE, ENTRY_MODIFY);
				out.println("Watcher service set on directory: " + mdDir);
			}

			out.println("Service started ... ");

			Predicate<Path> isWatchedFile = p -> {
				try {
					return Files.isSameFile(basketDef, p) || Files.isSameFile(marketData, p);
				} catch (Exception e) {
					return false;
				}
			};

			/* 
			 * Service's main loop
			 */
			while (true) {
				WatchKey key = service.take();
				boolean b = key.pollEvents()
					.stream()
					.filter(e -> e.kind() != OVERFLOW)
					.map(e -> ((WatchEvent<Path>) e).context())
					.filter(isWatchedFile)
					.findAny().isPresent();

				if (b) {
					valuate(basketDef, marketData, out);
				}
				
				if (!key.reset()) {
					break;
				}
			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	String renderLine(Map.Entry<Asset, Double> e) {
		String str = String.format("%-9s %15s", e.getKey().getName(), FORMAT.format(e.getValue()));		
		return str;
	}
	
	String renderTotals(DoubleSummaryStatistics stats) {
		String str = String.format("TOTALS    %15s", FORMAT.format(stats.getSum()));
		return str;
	}	
}
