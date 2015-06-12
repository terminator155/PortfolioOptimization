public class StockEntry
{
	public static final String STOCK_SYMBOL = "symbol";
	public static final String STOCK_DATE = "date";
	public static final String STOCK_OPEN = "open";
	public static final String STOCK_CLOSE = "close";
	public static final String STOCK_VARIATION = "variation";
	
	public static final String STOCK_MEAN = "mean";
	public static final String STOCK_VAR = "var";
	
	public String symbol;
	public int date;
	public double openPrice;
	public double closePrice;
	public double variation;
	public double mean;
	public double var;
}