
public class StockData {
	public String symbol;
	public float mean;
	public float variance;
	
	@Override
	public String toString()
	{
		return "<stock><symbol>"+this.symbol+"</symbol><mean>"+this.mean+"</mean><var>"+this.variance+"</var></stock>";
	}
}
