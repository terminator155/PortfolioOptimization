import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.bson.BasicBSONObject;

import com.mongodb.hadoop.io.BSONWritable;

import java.io.IOException;

public class StockStatReducer extends Reducer<Text, DoubleWritable, NullWritable,BSONWritable>
{
	@Override
	public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException
	{
		int count=0;
		double mean = 0;
		double variance = 0;
		double valFloat;
		
		for(DoubleWritable val : values)
		{
			valFloat = val.get();
			mean += valFloat;
			variance += valFloat * valFloat;
			
			count ++;
		}
		
		mean = mean / count;
		variance = (variance - mean*mean)/(count-1);
		
		BasicBSONObject obj = new BasicBSONObject();
		String symbol = key.toString();
		obj.put(StockEntry.STOCK_SYMBOL,symbol);
		obj.put(StockEntry.STOCK_MEAN, mean);
		obj.put(StockEntry.STOCK_VAR, variance);
		
		BSONWritable writable = new BSONWritable(obj);
		
		context.write(null, writable);
	}
}
