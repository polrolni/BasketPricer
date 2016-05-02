package mp.app;

/**
 * Resembles an asset in basket
 */
public class Asset {
	
	String name;

	String quoteName;
		
	double quantity;
	
	/**
	 * Constructor
	 * 
	 * @param name	asset's name
	 * @param quoteName	mapping to a quote name in market data 
	 * @param quantity	number of assets of that type in basket
	 */
	public Asset(String name, String quoteName, double quantity) {
		this.name = name;
		this.quoteName = quoteName;
		this.quantity = quantity;
	}

	public String getName() {
		return name;
	}
	
	public String getQuoteName() {
		return quoteName;
	}

	public double getQuantity() {
		return quantity;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
			+ "[" + "name=" + name + ",quantity=" + quantity + "]";
	}
	
	/**
	 * Syntax: quote_name quantity name
	 * 
	 * @param str	string representation of an asset in basket
	 * @return		basket component instance 
	 * @throws Exception	when parsing failed
	 */
	public static Asset parse(String str) throws Exception {
		try {
			String[] ts = str.split("\\s+");
			return new Asset(ts[2], ts[0], Double.parseDouble(ts[1]));
		} catch (Exception e) {
			throw new Exception("Parsing bucket component failed: " + str);
		}
	}
}
