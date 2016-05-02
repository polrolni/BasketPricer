package mp.app.measure;

import mp.app.Asset;
import mp.app.marketdata.MarketData;

public interface Measure {
	
	double calculate(Asset c, MarketData data);
}
