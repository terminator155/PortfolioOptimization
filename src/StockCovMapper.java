import java.io.IOException;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.bson.BSONObject;


public class StockCovMapper extends Mapper<Object, Object, Text, DoubleWritable> {
	@Override
	public void map(Object key, Object value, Context context) throws IOException, InterruptedException
	{
		BSONObject obj = (BSONObject)value;
		
		String sym1 = (String)obj.get(StockCov.STOCK_SYMBOL_1);
		String sym2 = (String)obj.get(StockCov.STOCK_SYMBOL_2);
		double val1 = (double)obj.get(StockCov.STOCK_VARIANCE_1);
		double val2 = (double)obj.get(StockCov.STOCK_VARIANCE_2);
		
		double val = val1 * val2;
		
		Text symbolKey = new Text(sym1+","+sym2);
		DoubleWritable writable = new DoubleWritable(val);
		context.write(symbolKey, writable);
	}
}
