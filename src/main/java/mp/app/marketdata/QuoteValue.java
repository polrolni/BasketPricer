package mp.app.marketdata;

/*
 * Representation of quote values (as of now price value only).
 */
public class QuoteValue {

	String name;
	
	double price;
		
	public QuoteValue(String name, double price) {
		this.name = name;
		this.price = price;
	}

	public String getName() {
		return name;
	}
	
	public double getPrice() {
		return price;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
			+ "[" + "name=" + name + ",price=" + price + "]";
	}

	/**
	 * Syntax: quote_name value
	 * 
	 * @param str	string representation of a quote
	 * @return		quote value instance 
	 * @throws Exception	when parsing failed
	 */
	public static QuoteValue parse(String str) throws Exception {
		String[] ts = str.split("\\s+");
		String name = ts[0];
		double price;

		try {
			price = Double.parseDouble(ts[1]);
		} catch (Exception e) {
			System.err.println(e + ", quote parsing failed: " + str);
			price = Double.NaN;
		}

		return new QuoteValue(name, price);
	}
}
