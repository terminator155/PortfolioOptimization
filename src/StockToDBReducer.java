import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.bson.BasicBSONObject;

import com.mongodb.hadoop.io.BSONWritable;

import java.io.IOException;

public class StockToDBReducer extends Reducer<Text,StockEntry,NullWritable,BSONWritable> {
	@Override public void reduce(final Text key, final Iterable<StockEntry> stocks,Context context) throws IOException, InterruptedException
	{
		BasicBSONObject obj = new BasicBSONObject();
		StockEntry entry = stocks.iterator().next();
		
		obj.put(StockEntry.STOCK_SYMBOL,entry.symbol);
		obj.put(StockEntry.STOCK_DATE,entry.date);
		obj.put(StockEntry.STOCK_OPEN,entry.openPrice);
		obj.put(StockEntry.STOCK_CLOSE,entry.closePrice);
		
		BSONWritable writable = new BSONWritable(obj);
		context.write(null, writable);
	}
}
