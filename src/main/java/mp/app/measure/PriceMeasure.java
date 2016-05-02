package mp.app.measure;

import mp.app.Asset;
import mp.app.marketdata.MarketData;
import mp.app.marketdata.QuoteValue;

public class PriceMeasure implements Measure {

	@Override
	public double calculate(Asset c, MarketData md) {
		QuoteValue qv = md.getQuote(c.getQuoteName());
		return qv != null
			? c.getQuantity() * qv.getPrice()
			: Double.NaN;
	}	
}
