import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.DoubleWritable;
import org.bson.BSONObject;

import java.io.IOException;

public class StockStatMapper extends Mapper<Object, Object, Text,DoubleWritable> {
	
	@Override
	public void map(Object key, Object value, Context context) throws IOException, InterruptedException
	{
		BSONObject obj = (BSONObject)value;
		
		String symbol = (String)obj.get(StockEntry.STOCK_SYMBOL);
		double variationFloat = (double)obj.get(StockEntry.STOCK_VARIATION);
		
		Text symbolKey = new Text(symbol);
		DoubleWritable variation = new DoubleWritable(variationFloat);
		context.write(symbolKey, variation);
	}
}
