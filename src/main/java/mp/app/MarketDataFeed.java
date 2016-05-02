package mp.app;

import static java.util.stream.Collectors.toCollection;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import mp.app.marketdata.MarketData;

/**
 * Market data feed is supposed to fetch newest 
 * market data and store them in a file.
 * 
 * The feed using the existing feed file 
 * as source of information which data should be 
 * requested from market data provider. Then, after the
 * data have been received, they will be persisted in that very file.
 */
public abstract class MarketDataFeed {
	
	static final String PARAM_FOLLOW = "-follow";

	static final String PARAM_DELAY = "-delay";

	/**
	 * Entry point of the execution
	 * 
	 * @param args
	 * @throws Exception
	 */
	public void execute(String[] args) throws Exception {
		boolean isFollow = false;
		int delay = 60;
		Path mkdata = null;

		/*
		 * Parsing input parameters
		 */
		List<String> list = new ArrayList<>(Arrays.asList(args));
		isFollow = list.remove(PARAM_FOLLOW);
		
		int i = list.indexOf(PARAM_DELAY);
		if (i >= 0) {
			try {
				delay = Integer.parseInt(list.get(i + 1));
				list.remove(i + 1);
				list.remove(i);				
			} catch (Exception e) {
				System.out.println(getUsage());				
				return;
			}
		}
		
		if (list.size() != 1) {
			System.out.println(getUsage());				
			return;			
		}
		
		String str = list.get(0);
		// try as regular file
		mkdata = Paths.get(str);
		if (!Files.isRegularFile(mkdata)) {
			// try as basket name
			mkdata = Paths.get(str + "." + BasketPricer.FILE_SUFFIX_MKDATA);
			if (!Files.isRegularFile(mkdata)) {
				String msg = "Unable to find market data file referenced by name: " + str;
				System.out.println(msg);
				return;
			}
		}
		/*
		 * End of parsing input parameters
		 */
		
		if (isFollow) {
			service(mkdata, delay, System.out);
		} else {
			execute(mkdata, System.out);
		}
	}
	
	/**
	 * Single execution of a data fetch
	 * 
	 * @param path path to input/output file
	 * @param log	log stream
	 * @throws Exception
	 */
	protected void execute(Path path, PrintStream log) throws Exception {
		/*
		 * Display initial information
		 */
		LocalDateTime start = LocalDateTime.now();
		log.println("Fetch starts at " + start.format(DateTimeFormatter.ISO_LOCAL_TIME));
		
		/*
		 * Load stock names
		 */
		Set<String> set = getQuoteNames(path);
		/*
		 * Fetch data
		 */
		Map<String, Double> map = fetch(set);
		
		/*
		 * Update file
		 */
		Utils.persistQuoteValuesWithLock(path, map, getClass().getSimpleName());
		
		/*
		 * Display statistics
		 */
		LocalDateTime end = LocalDateTime.now();
		long seconds = Duration.between(start, end).getSeconds();
		log.println(
				"Fetch ends at " + end.format(DateTimeFormatter.ISO_LOCAL_TIME) 
			+	", duration: " + seconds + "s"
			+ 	", assets: " + map.size());
		log.println();
	}
	
	/**
	 * Provider-dependent logic to execute fetch of market data
	 * 
	 * @param names	assets names
	 * @return	map of asset name - value pairs
	 * @throws Exception	
	 */
	protected abstract Map<String, Double> fetch(Set<String> names) throws Exception;

	/**
	 * Starts the fetcher in a continuous mode, used scheduled executor to 
	 * execute the runs.
	 * 
	 * @param path	path to input/output file
	 * @param delay	delay in seconds between executions
	 * @param log	log console
	 * @throws Exception
	 */
	protected void service(Path path, int delay, PrintStream log) throws Exception {
		ScheduledExecutorService service = null;

		service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleWithFixedDelay(
			() -> { 
				try { 
					execute(path, log);
				} catch (Exception e) {
					log.println(e);
				}
			}, 
			0, 
			delay, 
			TimeUnit.SECONDS);
	}

	/**
	 * Reuses results file as definition for the stock symbols of interest
	 * 
	 * @param path	feed file
	 * @return	set of stock symbols
	 */
	protected Set<String> getQuoteNames(Path path) {
		MarketData md = Utils.getMarketDataWithLock(path);
		return md.getQuotes()
			.stream()
			.map(s -> s.getName())
			.collect(toCollection(TreeSet::new));
	}
	
	/**
	 * @return usage information
	 */
	protected String getUsage() {
		return  "Usage: java " + this.getClass().getName() + " [-options]" + " basket_name" + "\n"
			+	"   or: java " + this.getClass().getName() + " [-options]" + " marketdata_file" + "\n"
			+	"\n"
			+	"Parameters:" + "\n"
			+	"   basket_name      market data file will be expected in <current_dir>/<basket_name>." + BasketPricer.FILE_SUFFIX_MKDATA + "\n"
			+	"   marketdata_file  market data file" + "\n"
			+	"\n"
			+	"Options:" + "\n"
			+	"   -follow          continuous mode, program run infinitely and periodicaly schedules market data update" + "\n"
			+	"   -delay <seconds> delay in seconds betweed market data updates (default 60)" + "\n"
			;
	}
}
