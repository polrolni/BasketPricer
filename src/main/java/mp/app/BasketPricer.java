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
 * Simple pricing for a basket of assets.
 * 
 * It is an attempt to make use of java SE 8 parallelization 
 * provided by java streams.
 */
public class BasketPricer {

	static final NumberFormat REPORT_NUMBERS_FORMAT = new DecimalFormat("#,###,##0.00");

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

		/*
		 * Parsing input parameters
		 */
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
		/*
		 * End of parsing input parameters
		 */
		
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
	 * @return	valuation totals
	 */
	public double valuate(Path basketDef, Path marketData, PrintStream out) {
		Set<Asset> basket = Utils.getBasket(basketDef);
		MarketData md = Utils.getMarketDataWithLock(marketData);
		Measure measure = new PriceMeasure();

		/*
		 * Display initial information
		 */
		out.println("Valuation date-time:    " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		out.println("Current directory:      " + Paths.get(".").toAbsolutePath().normalize());
		out.println("Basket definition file: " + basketDef);
		out.println("Market data file:       " + marketData);
		out.println();
		
		/*
		 * Pricing
		 */
		Map<Asset, Double> map = valuate(basket, md, measure);

		/*
		 * Reporting
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
		
		return stats.getSum();
	}

	/**
	 * Valuation logic, parallelization of computations done via parallel stream.
	 * 
	 * @param basket	assets basket
	 * @param md		market data
	 * @param measure	measure to calculate
	 * @return	valuation results
	 */
	public Map<Asset, Double> valuate(Set<Asset> basket, MarketData md, Measure measure) {
		Map<Asset, Double> map = basket
			.parallelStream()
			.collect(toConcurrentMap(
				Function.identity(),
				c -> measure.calculate(c, md)));
		return map;
	}
	
	/**
	 * Logic for continuous mode. It uses java watch service to trace changes
	 * in basket and feed files and - if change occurs - it triggers
	 * revaluation of the basket.
	 * 
	 * @param basketDef
	 * @param marketData
	 * @param out
	 */
	@SuppressWarnings("unchecked")
	public void startService(Path basketDef, Path marketData, PrintStream out) {		
		try (WatchService service = FileSystems.getDefault().newWatchService()) {
			Path btDir = basketDef.toAbsolutePath().getParent();
			btDir.register(service, ENTRY_CREATE, ENTRY_MODIFY);
			out.println("Watcher service set on directory: " + btDir);
			
			Path mdDir = marketData.toAbsolutePath().getParent();
			if (!Files.isSameFile(btDir, mdDir)) {
				mdDir.register(service, ENTRY_CREATE, ENTRY_MODIFY);
				out.println("Watcher service set on directory: " + mdDir);
			}

			out.println("Service started ... ");

			Predicate<Path> isWatchedFile = p -> {
				try {
					// check basket file
					Path ap = btDir.resolve(p);
					if (Files.isSameFile(basketDef, ap))
						return true;

					// check market data file
					ap = mdDir.resolve(p);
					return Files.isSameFile(marketData, ap);
				} catch (Exception e) {
					return false;
				}
			};

			/* *************************
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
			/* 
			 * End service's main loop
			 * ************************* */
		} catch (Exception e) {
			System.err.println(e);
		}
	}
	
	String renderLine(Map.Entry<Asset, Double> e) {
		String str = String.format("%-9s %15s", e.getKey().getName(), REPORT_NUMBERS_FORMAT.format(e.getValue()));		
		return str;
	}
	
	String renderTotals(DoubleSummaryStatistics stats) {
		String str = String.format("TOTALS    %15s", REPORT_NUMBERS_FORMAT.format(stats.getSum()));
		return str;
	}	
}
