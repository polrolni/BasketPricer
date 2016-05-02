package mp.app;

import static java.util.stream.Collectors.toCollection;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
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
import java.util.stream.Stream;

import mp.app.marketdata.MarketData;

/**
 * Uses file-backed market data definition and storage
 */
public abstract class MarketDataFeed {

	static final NumberFormat FORMAT;
	
	static final String PARAM_FOLLOW = "-follow";

	static final String PARAM_DELAY = "-delay";
	
	static {
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		dfs.setGroupingSeparator(',');
		dfs.setNaN("NaN");
		FORMAT = new DecimalFormat("0.00000", dfs);
	}

	public void execute(String[] args) throws Exception {
		boolean isFollow = false;
		int delay = 60;
		Path mkdata = null;

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
		
		if (isFollow) {
			service(mkdata, delay, System.out);
		} else {
			execute(mkdata, System.out);
		}
	}
	
	protected void execute(Path path, PrintStream out) throws Exception {
		/*
		 * Display initial information
		 */
		LocalDateTime start = LocalDateTime.now();
		out.println("Fetch starts at " + start.format(DateTimeFormatter.ISO_LOCAL_TIME));
		
		/*
		 * Load stock names
		 */
		Set<String> set = load(path);
		/*
		 * Fetch data
		 */
		Map<String, Double> map = fetch(set);
		
		/*
		 * Update file
		 */
		saveWithLock(path, map);
		
		/*
		 * Display statistics
		 */
		LocalDateTime end = LocalDateTime.now();
		long seconds = Duration.between(start, end).getSeconds();
		out.println(
				"Fetch ends at " + end.format(DateTimeFormatter.ISO_LOCAL_TIME) 
			+	", duration: " + seconds + "s"
			+ 	", assets: " + map.size());
		out.println();
	}
		
	protected void service(Path path, int delay, PrintStream out) throws Exception {
		ScheduledExecutorService service = null;

			service = Executors.newSingleThreadScheduledExecutor();
			service.scheduleWithFixedDelay(
				() -> { 
					try { 
						execute(path, out);
					} catch (Exception e) {
						out.println(e);
					}
				}, 
				0, 
				delay, 
				TimeUnit.SECONDS);
	}

	/**
	 * Reuses results file as definition for the stock symbols of interest
	 * 
	 * @param path	location of the file defining market data
	 * @return	set of stock symbols
	 */
	protected Set<String> load(Path path) {
		MarketData md = Utils.getMarketDataWithLock(path);
		return md.getQuotes()
			.stream()
			.map(s -> s.getName())
			.collect(toCollection(TreeSet::new));
	}
	
	protected abstract Map<String, Double> fetch(Set<String> names) throws Exception;

	protected void saveWithLock(Path path, Map<String, Double> map) throws IOException {
		try (
			FileOutputStream fos = new FileOutputStream(path.toFile(), false);
			PrintStream out = new PrintStream(fos);
			FileChannel channel = fos.getChannel();
		) {
			channel.lock();

			Stream.of(
					"################################	"
				,	"# Market Data File					"
				, 	"# Source: " + getClass().getSimpleName()
				,	"# Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
				,	"#									"	
				,	"# Syntax: quote_name quote_value	"
				, 	"################################ 	"
			).forEach(out::println);

			out.println();
			
			map.entrySet().stream()
				.map(MarketDataFeed::renderLine)
				.forEach(out::println);
		}
	}
	
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

	static String renderLine(Map.Entry<String, Double> e) {
		String str = String.format("%-10s %s", e.getKey(), FORMAT.format(e.getValue()));		
		return str;
	}
}
