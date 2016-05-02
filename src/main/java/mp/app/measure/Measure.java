package mp.app.measure;

import mp.app.Asset;
import mp.app.marketdata.MarketData;

/**
 * A measure represents a formula to calculate certain value
 * by using asset's definition and recent market data.
 */
public interface Measure {
	
	/**
	 * Executes calculations
	 * 
	 * @param asset	asset definition
	 * @param data	market data
	 * @return	calculated value 
	 * 			or Double.NaN when calculation failed
	 */
	double calculate(Asset asset, MarketData data);
}
