import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.NullWritable;

import com.mongodb.hadoop.io.BSONWritable;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.io.IOException;
import java.util.StringTokenizer;

public class StockCovReducer extends Reducer<Text, DoubleWritable, NullWritable, BSONWritable> {
	@Override
	public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException
	{
		int count = 0;
		double variation = 0;
		for(DoubleWritable val : values)
		{
			count++;
			variation += val.get();
		}
		
		variation/= count;
		
		StringTokenizer tokenizer = new StringTokenizer(key.toString(),",");
		String sym1 = tokenizer.nextToken();
		String sym2 = tokenizer.nextToken();
		BasicBSONObject obj = new BasicBSONObject();
		obj.put(StockCov.STOCK_SYMBOL_1, sym1);
		obj.put(StockCov.STOCK_SYMBOL_2, sym2);
		obj.put(StockCov.STOCK_COV, variation);
		
		BSONWritable writable = new BSONWritable(obj);
		
		context.write(null, writable);
	}
}
