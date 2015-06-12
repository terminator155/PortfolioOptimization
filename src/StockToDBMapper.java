import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.io.Text;

import java.io.IOException;
import java.util.StringTokenizer;

public class StockToDBMapper extends Mapper<Object,Text,Text,StockEntry> 
{
	@Override
	public void map(Object key, Text value, Context context) throws IOException, InterruptedException
	{
		String line = value.toString();
		
		StringTokenizer tokenizer = new StringTokenizer(line,",");
		//MetaStock format:
		//Symbol, Date, Open, High, Low, Close, Volume
		String symbol = tokenizer.nextToken();
		String strDate = tokenizer.nextToken();
		String strOpen = tokenizer.nextToken();
		tokenizer.nextToken();
		tokenizer.nextToken();
		String strClose = tokenizer.nextToken();
		
		int valDate = Integer.parseInt(strDate);
		float valOpen = Float.parseFloat(strOpen);
		float valClose = Float.parseFloat(strClose);
		
		StockEntry entry = new StockEntry();
		entry.symbol = symbol;
		entry.date = valDate;
		entry.openPrice = valOpen;
		entry.closePrice = valClose;
		
		Text lineT = new Text(line);
		
		context.write(lineT, entry);
	}
}
