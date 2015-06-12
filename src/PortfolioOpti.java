import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.io.File;
import java.util.Set;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;

import com.mongodb.hadoop.MongoOutputFormat;
import com.mongodb.hadoop.io.BSONWritable;
import com.mongodb.hadoop.MongoInputFormat;
import com.mongodb.hadoop.util.MongoConfigUtil;
import com.mongodb.MongoClient;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;

import org.ejml.simple.SimpleMatrix;


public class PortfolioOpti {
	public static final String HADOOP_WORK_DIR = "/portfolio/";
	public static final String HADOOP_OUT_TEMP_DIR = "/portfolioResult/";
	public static final String HADOOP_JAR_DIR = "/portfolioJar/";
	
	public static final String DB_PORTFOLIO = "PortfolioOpti";
	public static final String DB_TABLE_STOCK = "tableStock";
	public static final String DB_TABLE_STAT = "tableStat";
	public static final String DB_TABLE_COV_QUERY = "tableCovQuery";
	public static final String DB_TABLE_COV = "tableCov";
	
	public static final String JAR_MONGO_JAVA = "/home/zlw/workspace/PortfolioOpti/mongo-java-driver-2.13.0.jar";
	public static final String JAR_EJML_CORE = "/home/zlw/workspace/PortfolioOpti/ejml/EJML-core-0.26.jar";
	
	public static final int PROG_LOAD_FILE = 50;
	
	private MongoClient mClient = null;
	private DB mDB = null;
	private PortfolioSelect selecter;
	private PortfolioFrame frameStock;
	OptiProgressIndicator progIndicator;
	private OptiAnalyse optimiser;
	
	private DBCollection getDBCollection(String name)
	{
		if(this.mClient == null)
		{
			mDB = null;
			try {
				mClient = new MongoClient("localhost",27017);
			} catch (UnknownHostException e) {
				
				e.printStackTrace();
				return null; 
			}
		}
		
		if(mDB == null)
		{
			mDB = mClient.getDB(DB_PORTFOLIO);
		}
		
		return mDB.getCollection(name);
	}
	
	public static void main(String args[]) throws Exception
	{	
		PortfolioFrame frame = new PortfolioFrame();
		PortfolioOpti opti = new PortfolioOpti();
		frame.optimizer = opti;
		opti.frameStock = frame;
		opti.progIndicator = frame;
		
		//int exitCode = ToolRunner.run(new PortfolioOpti(), args);
		//System.exit(exitCode);
		
		/*PortfolioOpti opti = new PortfolioOpti();
		ArrayList<StockEntry> list = opti.eliminateInefficientStocks(20);
		System.out.println("Stock count: " + list.size());
		for(StockEntry entry : list)
			System.out.println("Symbol:" + entry.symbol+ " , mean = "+entry.mean + " , var = "+entry.var);*/
	}
	
	public void propagateDBCovQuery(ArrayList<StockEntry> list,ArrayList<Integer> dates)
	{
		int dateCount = dates.size();
		int len = list.size();
		ArrayList<String> symbolList = new ArrayList<String>();
		for(int i=0;i<len;i++)
			symbolList.add(list.get(i).symbol);
		
		DBCollection stocks = this.getDBCollection(DB_TABLE_STOCK);
		DBCursor cursor;
		
		BasicDBObject query;
		ArrayList<DBObject> dataToInsert;
		ArrayList<StockEntry> dailyData;
		StockEntry entry;
		DBObject obj;
		BasicDBObject objToInsert;
		
		DBCollection covQueryTable = this.getDBCollection(DB_TABLE_COV_QUERY);
		
		for(int k=0;k<dateCount;k++)
		{
			query = new BasicDBObject();
			query.put(StockEntry.STOCK_SYMBOL, new BasicDBObject("$in",symbolList));
			query.put(StockEntry.STOCK_DATE, dates.get(k));
			
			cursor = stocks.find(query);
			dailyData = new ArrayList<StockEntry>();
			while(cursor.hasNext())
			{
				entry = new StockEntry();
				obj = cursor.next();
				
				entry.symbol = (String)obj.get(StockEntry.STOCK_SYMBOL);
				entry.variation = (double)obj.get(StockEntry.STOCK_VARIATION);
				dailyData.add(entry);
			}
			
			len = dailyData.size();
			dataToInsert = new ArrayList<DBObject>();
			for(int i=0;i<len;i++)
			{
				for(int j=0;j<len;j++)
				{
					objToInsert = new BasicDBObject();
					objToInsert.put(StockCov.STOCK_SYMBOL_1, dailyData.get(i).symbol);
					objToInsert.put(StockCov.STOCK_SYMBOL_2, dailyData.get(j).symbol);
					objToInsert.put(StockCov.STOCK_VARIANCE_1, dailyData.get(i).variation);
					objToInsert.put(StockCov.STOCK_VARIANCE_2, dailyData.get(j).variation);
					
					dataToInsert.add(objToInsert);
				}
			}
			covQueryTable.insert(dataToInsert);
		}
	}
	
	public ArrayList<StockEntry> getStockEspVar(ArrayList<StockEntry> list)
	{
		ArrayList<StockEntry> result = new ArrayList<StockEntry>();
		
		int count = list.size();
		StockEntry entry;
		ArrayList<String> symbolList = new ArrayList<String>();
		for(int i=0;i<count;i++)
		{
			entry = new StockEntry();
			entry.symbol = list.get(i).symbol;
			result.add(entry);
			
			symbolList.add(list.get(i).symbol);
		}
		
		DBCollection statDB = this.getDBCollection(DB_TABLE_STAT);
		BasicDBObject query = new BasicDBObject();
		query.put(StockEntry.STOCK_SYMBOL, new BasicDBObject("$in",symbolList));
		DBCursor cursor;
		DBObject obj;
		String sym;
		
		cursor = statDB.find(query);
		
		int i;
		while(cursor.hasNext())
		{
			obj = cursor.next();
			sym = (String)obj.get(StockEntry.STOCK_SYMBOL);
			
			for(i=0;i<count;i++)
			{
				if(sym.equals(result.get(i).symbol))
					break;
			}
			entry = result.get(i);
			
			entry.mean = (double)obj.get(StockEntry.STOCK_MEAN);
			entry.var = (double)obj.get(StockEntry.STOCK_VAR);
		}
		
		return result;
	}
	
	public double[][] getStockCov(ArrayList<StockEntry> list)
	{
		int count = list.size();
		double cov[][] = new double[count][count];
		
		DBCollection covDB = this.getDBCollection(DB_TABLE_COV);
		DBCursor cursor;
		DBObject obj;
		String sym1,sym2;
		int idx1,idx2;
		StockEntry entry;

		ArrayList<StockEntry> meanList = this.getStockEspVar(list);
		
		double mean1,mean2;
		cursor = covDB.find();
		
		while(cursor.hasNext())
		{
			obj = cursor.next();
			sym1 = (String)obj.get(StockCov.STOCK_SYMBOL_1);
			sym2 = (String)obj.get(StockCov.STOCK_SYMBOL_2);
			
			idx1 = 0;
			idx2 = 2;
			for(int i=0;i<count;i++)
			{
				entry = list.get(i);
				
				if(entry.symbol.equals(sym1))
					idx1 = i;
				if(entry.symbol.equals(sym2))
					idx2 = i;
			}
			
			mean1 = 0;
			mean2 = 0;
			for(int i=0;i<count;i++)
			{
				entry = meanList.get(i);
				
				if(entry.symbol.equals(sym1))
					mean1 = entry.mean;
				if(entry.symbol.equals(obj))
					mean2 = entry.mean;
			}
			
			cov[idx1][idx2] = (double)obj.get(StockCov.STOCK_COV) - mean1 * mean2;
		}
		
		return cov;
	}
	
	public ArrayList<Integer> getDateList(String symbol)
	{
		ArrayList<Integer> result = new ArrayList<Integer>();
		DBCollection collection = this.getDBCollection(DB_TABLE_STOCK);
		
		BasicDBObject query = new BasicDBObject();
		query.put(StockEntry.STOCK_SYMBOL, symbol);
		DBCursor cursor = collection.find(query);
		
		while(cursor.hasNext())
		{
			result.add((Integer)cursor.next().get(StockEntry.STOCK_DATE));
		}
		
		return result;
	}
	
	public ArrayList<StockEntry> eliminateInefficientStocks(int remainingCount)
	{
		DBCollection collection = this.getDBCollection(DB_TABLE_STAT);
		
		DBCursor cursor = collection.find();
		cursor.sort(new BasicDBObject(StockEntry.STOCK_MEAN,1));//sort result in ascending order, -1 for descending order
		
		DBObject obj;
		ArrayList<StockEntry> list = new ArrayList<StockEntry>();
		StockEntry current, next;
		
		while(cursor.hasNext())
		{
			obj = cursor.next();
			current = new StockEntry();
			
			current.symbol = (String)obj.get(StockEntry.STOCK_SYMBOL);
			current.mean = (double)obj.get(StockEntry.STOCK_MEAN);
			current.var = (double)obj.get(StockEntry.STOCK_VAR);
			list.add(current);
			//System.out.println("Symbol:" + current.symbol+ " , mean = "+current.mean);
		}
		
		for(int i=0;i<list.size()-1;)
		{
			current = list.get(i);
			next = list.get(i+1);
			
			if(next.var<=current.var)
			{
				list.remove(i);//the current one
				if(i>0)
					i--;
			}
			else
			{
				i ++;
			}
			//System.out.println("List length: "+list.size());
		}
		
		if(remainingCount <=0)
			return list;
		
		int size = list.size();
		if(size < remainingCount)
			return list;
		
		ArrayList<StockEntry> result = new ArrayList<StockEntry>();
		result.addAll(list.subList(size - 1 - remainingCount, size - 1));
		
		return result;
	}
	
	public void loadTextFileToDB(String localPath) throws IOException
	{
		DBCollection collection = this.getDBCollection(PortfolioOpti.DB_TABLE_STOCK);
		
		FileReader fReader = new FileReader(localPath);
		BufferedReader reader = new BufferedReader(fReader);
		
		ArrayList<DBObject> objectList = new ArrayList<DBObject>();
		
		String line;
		
		while((line = reader.readLine()) != null)
		{
			DBObject obj = PortfolioOpti.getDBStockEntryFromString(line);
			objectList.add(obj);
		}
		
		collection.insert(objectList);
		reader.close();
	}
	
	public void loadInputFolderToDB(String localDir) throws IOException
	{
		File folder = new File(localDir);
		File[] files = folder.listFiles();
		
		int count = files.length;
		int i=0;
		int delta = PortfolioOpti.PROG_LOAD_FILE/count;
		
		for(File f : files)
		{
			this.progIndicator.displayProgress(delta * i, String.format("Loading %dth file", i+1));
			this.loadTextFileToDB(f.getAbsolutePath());
			i++;
		}
	}
	
	public static BasicDBObject getDBStockEntryFromString(String line)
	{
		BasicDBObject obj = new BasicDBObject();
		
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
		float variation = (valOpen - valClose)/valOpen;
		
		obj.put(StockEntry.STOCK_SYMBOL,symbol);
		obj.put(StockEntry.STOCK_DATE,valDate);
		obj.put(StockEntry.STOCK_OPEN,valOpen);
		obj.put(StockEntry.STOCK_CLOSE,valClose);
		obj.put(StockEntry.STOCK_VARIATION, variation);
		
		return obj;
	}
	
	public void displayStockStat(List<StockEntry> data)
	{
		if(this.frameStock != null)
		{
			this.frameStock.loadStatDataToTable(data);
		}
	}
	
	class PortfolioSelect extends Configured implements Tool, Runnable
	{
		String inputPath;
		PortfolioOpti opti;
		OptiProgressIndicator progIndicator;
		
		@Override
		public int run(String[] arg0) throws Exception {
			Configuration conf = this.getConf();
			
			conf.addResource("/usr/local/hadoop/etc/hadoop/core-site.xml");
			conf.addResource("/usr/local/hadoop/etc/hadoop/hdfs-site.xml");
			
			System.out.println("Loading input data");
			this.progIndicator.displayProgress(0, "Loading input data");
			
			this.opti.getDBCollection(DB_TABLE_STAT).remove(new BasicDBObject());
			this.opti.getDBCollection(DB_TABLE_STOCK).remove(new BasicDBObject());
			
			opti.loadInputFolderToDB(inputPath);
			
			System.out.println("Input file all loaded");
			this.progIndicator.displayProgress(PROG_LOAD_FILE, "Config Map-Reduce procedure");
			
			FileSystem fs = FileSystem.get(conf);
			
			Path localJar = new Path(JAR_MONGO_JAVA);
			Path hdfsJar = new Path(HADOOP_JAR_DIR+"mongo-java-driver-2.13.0.jar");
			
			fs.copyFromLocalFile(localJar, hdfsJar);
			
			Path localEJML = new Path(JAR_EJML_CORE);
			Path hdfsEJML = new Path(HADOOP_JAR_DIR+"EJML-core-0.26.jar");
			
			fs.copyFromLocalFile(localEJML, hdfsEJML);
			
			MongoConfigUtil.setOutputURI(conf, "mongodb://localhost:27017/"+DB_PORTFOLIO+"."+DB_TABLE_STAT);
			MongoConfigUtil.setInputURI(conf, "mongodb://localhost:27017/"+DB_PORTFOLIO+"."+DB_TABLE_STOCK);
			
			//Job1: experience and variance
			Job jobExp = Job.getInstance(conf, "PortfolioOpti");
			jobExp.setJarByClass(PortfolioOpti.class);
			jobExp.setMapperClass(StockStatMapper.class);
			jobExp.setReducerClass(StockStatReducer.class);
			
			jobExp.setMapOutputKeyClass(Text.class);
			jobExp.setMapOutputValueClass(DoubleWritable.class);
			
			jobExp.setOutputKeyClass(NullWritable.class);
			jobExp.setOutputValueClass(BSONWritable.class);
			
			jobExp.setInputFormatClass(MongoInputFormat.class);
			jobExp.setOutputFormatClass(MongoOutputFormat.class);
			
			jobExp.addFileToClassPath(hdfsJar);
			jobExp.addFileToClassPath(hdfsEJML);
			
			this.progIndicator.displayProgress(PROG_LOAD_FILE+10, "Executing statistical analyse with Map-Reduce");
			
			jobExp.waitForCompletion(true);
			
			this.progIndicator.displayProgress(100, "Bourse selection finie");
			
			return 0;
		}

		@Override
		public void run() {
			try {
				ToolRunner.run(this, null);
				List<StockEntry> data = this.opti.eliminateInefficientStocks(-1);
				this.opti.displayStockStat(data);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void execPortfolioElemination(String inputPath) throws Exception
	{
		this.selecter = new PortfolioSelect();
		this.selecter.inputPath = inputPath;
		this.selecter.opti = this;
		this.selecter.progIndicator = this.progIndicator;
		Thread th = new Thread(this.selecter);
		th.start();
	}
	
	class OptiAnalyse extends Configured implements Tool, Runnable
	{
		OptiProgressIndicator progIndicator;
		PortfolioOpti opti;
		double[] esperance = null;
		ArrayList<double[]> proportions = null;
		double[] varianceLevels = null;
		ArrayList<StockEntry> stocks = null;
		
		@Override
		public void run() {
			try {
				ToolRunner.run(this, null);
				this.opti.displayOptiResult(stocks, esperance,varianceLevels, proportions);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public int run(String[] arg0) throws Exception {
			Configuration conf = this.getConf();
			
			conf.addResource("/usr/local/hadoop/etc/hadoop/core-site.xml");
			conf.addResource("/usr/local/hadoop/etc/hadoop/hdfs-site.xml");
	
			this.progIndicator.displayProgress(0, "Preparing for covariance analysis");
			
			FileSystem fs = FileSystem.get(conf);
			
			Path localJar = new Path(JAR_MONGO_JAVA);
			Path hdfsJar = new Path(HADOOP_JAR_DIR+"mongo-java-driver-2.13.0.jar");
			
			fs.copyFromLocalFile(localJar, hdfsJar);
			
			Path localEJML = new Path(JAR_EJML_CORE);
			Path hdfsEJML = new Path(HADOOP_JAR_DIR+"EJML-core-0.26.jar");
			
			fs.copyFromLocalFile(localEJML, hdfsEJML);
			
			ArrayList<StockEntry> list = this.stocks;
			
			DBCollection stocks = this.opti.getDBCollection(DB_TABLE_STOCK);
			BasicDBObject query = new BasicDBObject();
			query.put(StockEntry.STOCK_SYMBOL, list.get(0).symbol);
			DBCursor cursor = stocks.find(query);
			
			ArrayList<Integer> dates = new ArrayList<Integer>();
			while(cursor.hasNext())
			{
				dates.add((Integer)cursor.next().get(StockEntry.STOCK_DATE));
			}
			this.progIndicator.displayProgress(10, "Preparing data for Map-Reduce covariance analysis");
			
			this.opti.propagateDBCovQuery(list, dates);
			
			MongoConfigUtil.setOutputURI(conf, "mongodb://localhost:27017/"+DB_PORTFOLIO+"."+DB_TABLE_COV);
			MongoConfigUtil.setInputURI(conf, "mongodb://localhost:27017/"+DB_PORTFOLIO+"."+DB_TABLE_COV_QUERY);
			
			this.progIndicator.displayProgress(30, "Config Map-Reduce procedure");
			
			Job jobCov = Job.getInstance(conf, "PortfolioOpti");
			jobCov.setJarByClass(PortfolioOpti.class);
			jobCov.setMapperClass(StockCovMapper.class);
			jobCov.setReducerClass(StockCovReducer.class);
			
			jobCov.setMapOutputKeyClass(Text.class);
			jobCov.setMapOutputValueClass(DoubleWritable.class);
			
			jobCov.setOutputKeyClass(NullWritable.class);
			jobCov.setOutputValueClass(BSONWritable.class);
			
			jobCov.setInputFormatClass(MongoInputFormat.class);
			jobCov.setOutputFormatClass(MongoOutputFormat.class);
			
			jobCov.addFileToClassPath(hdfsJar);
			jobCov.addFileToClassPath(hdfsEJML);
			
			this.progIndicator.displayProgress(60, "Analysing covariance of stocks");
			jobCov.waitForCompletion(true);
			this.progIndicator.displayProgress(80, "Covariance analyse finie");
			
			double cov[][] = this.opti.getStockCov(list);
			int count = list.size();
			
			System.out.format("%10s", " ");
			for(int i=0;i<count;i++)
				System.out.format("%10s", list.get(i).symbol);
			System.out.print("\n");
			
			for(int i=0;i<count;i++)
			{
				System.out.format("%10s", list.get(i).symbol);
				
				for(int j=0;j<count;j++)
				{
					if(cov[i][j] <=0)
						cov[i][j]*= -1;
					System.out.format("%10f", cov[i][j]);
				}
				
				System.out.format("\n");
			}
			this.progIndicator.displayProgress(81, "Optimisation en cours");
			ArrayList<StockEntry> expCov = this.opti.getStockEspVar(list);
			this.proportions = this.optimizePartition(cov, expCov);
			this.evaluateEspVarLevels(proportions, expCov);
			
			this.progIndicator.displayProgress(95, "Cleaning up temp data");
			this.opti.getDBCollection(DB_TABLE_STOCK).remove(new BasicDBObject());
			this.opti.getDBCollection(DB_TABLE_COV).remove(new BasicDBObject());
			this.opti.getDBCollection(DB_TABLE_COV_QUERY).remove(new BasicDBObject());
			this.progIndicator.displayProgress(100, "Portfolio optimisation finie");
			
			return 0;
		}
		
		void evaluateEspVarLevels(ArrayList<double[]> proportions,ArrayList<StockEntry> expCov)
		{
			int levelCount = proportions.size();
			this.esperance = new double[levelCount];
			this.varianceLevels = new double[levelCount];
			
			StockEntry entry;
			int count = expCov.size();
			double prop[] = null;
			for(int i=0;i<levelCount;i++)
			{
				prop = proportions.get(i);
				this.esperance[i] = 0;
				this.varianceLevels[i] = 0;
				
				for(int j=0;j<count;j++)
				{
					entry = expCov.get(j);
					this.esperance[i] += prop[j]*entry.mean;
					this.varianceLevels[i] += prop[j]*prop[j]*entry.var;
				}
			}
		}
		
		ArrayList<double[]> optimizePartition(double cov[][], ArrayList<StockEntry> expVar)
		{
			/*double newCov[][] = {{0.146,0.0187,0.0145},{0.0187,0.0854,0.0104},{0.0145,0.0104,0.0289}};
			expVar = new ArrayList<StockEntry>();
			StockEntry e = new StockEntry();
			e.symbol = "A";
			e.mean = 0.062;
			expVar.add(e);
			
			e = new StockEntry();
			e.symbol = "B";
			e.mean = 0.146;
			expVar.add(e);
			
			e = new StockEntry();
			e.symbol = "C";
			e.mean = 0.128;
			expVar.add(e);*/
			ArrayList<double[]> proportion = new ArrayList<double[]>();
			
			SimpleMatrix covMat = new SimpleMatrix(cov);
			System.out.println("cov matrix:");
			covMat.print();
			
			int count = expVar.size();
			double [][] mean = new double[count][1];
			
			StockEntry entry;
			
			SimpleMatrix matA = new SimpleMatrix(1,count);
			matA.set(1);
			System.out.println("matA matrix:");
			matA.print();
			
			SimpleMatrix matM = new SimpleMatrix(count+1,count+1);
			for(int i=0;i<count;i++)
			{
				for(int j=0;j<count;j++)
				{
					matM.set(i, j, covMat.get(i, j));
				}
				matM.set(count, i, matA.get(0, i));
				matM.set(i, count, matA.get(0, i));
			}
			System.out.println("matM matrix:");
			matM.print();
			
			this.esperance = new double[count];
			for(int i=0;i<count;i++)
			{
				entry = expVar.get(i);
				mean[i][0] = entry.mean;
				this.esperance[i] = entry.mean;
			}
			
			//SimpleMatrix meanMat = new SimpleMatrix(mean);
			
			SimpleMatrix matS = new SimpleMatrix(count+1,1);
			for(int i=0;i<count;i++)
				matS.set(i, 0, mean[i][0]);
			System.out.println("matS matrix:");
			matS.print();
			
			SimpleMatrix matR = new SimpleMatrix(count+1,1);
			matR.set(count, 0, 1);
			System.out.println("matR matrix:");
			matR.print();
			
			double max;
			int maxIdx = 0;
			
			max = mean[0][0];
			double [] prop = new double[count];
			for(int i=1;i<count;i++)
			{
				prop[i] = 0;
				
				if(mean[i][0]>max)
				{
					maxIdx = i;
					max = mean[i][0];
				}
			}
			
			HashSet<Integer> inSet = new HashSet<Integer>();
			inSet.add(maxIdx);
			prop[maxIdx] = 1;
			proportion.add(prop);
			
			boolean fini = false;
			double lambdaLimit = -1;
			//int maxIter = 3;
			//int iter = 0;
			
			SimpleMatrix matMT = matM.copy();
			for(int i=0;i<count;i++)
			{
				if(inSet.contains(i) == false)
					matMT = this.setMatrixCross(i, i, 1, matMT);
			}
			System.out.println("matMT matrix:");
			matMT.print();
			
			SimpleMatrix matNT = matMT.invert();
			for(int i=0;i<count;i++)
			{
				if(inSet.contains(i) == false)
					matNT = this.setMatrixCross(i, i, 0, matNT);
			}
			System.out.println("matNT matrix:");
			matNT.print();
			
			SimpleMatrix matTT = matNT.mult(matR);
			System.out.println("matTT matrix:");
			matTT.print();
			
			SimpleMatrix matUT = matNT.mult(matS);
			
			System.out.println("matUT matrix:");
			matUT.print();
			
			int lastId = maxIdx;
			SimpleMatrix matRow;
			int iter = 0;
			while(!fini)
			{			
				iter++;
				//testing if line can be "in"
				max = -1;
				double lambdaE = 0;
				for(int i=0;i<count;i++)
				{
					if(inSet.contains(i))
					{
						SimpleMatrix matV = matM.mult(matTT);
						SimpleMatrix matW = matM.mult(matUT).minus(matS);
						lambdaE = (0-matV.get(i, 0))/matW.get(i, 0);
					}
					else
					{
						matRow = matM.extractVector(true, i);
						//System.out.println("matRow matrix:");
						//matRow.print();
						
						lambdaE = ((matRow.mult(matTT)).get(0, 0))/(mean[i][0] - (matRow.mult(matUT)).get(0, 0));
					}
					System.out.println("LambdaE: "+lambdaE+"max :"+max+", lambda limit: "+lambdaLimit+", i= "+i+" lastId= "+lastId);
					if(lambdaE > max && i != lastId && !Double.isNaN(lambdaE))
					{
						if(lambdaLimit == -1 || lambdaLimit > lambdaE)
						{
							max = lambdaE;
							maxIdx = i;
						}
					}
				}

				System.out.println("max lambda: "+max+", MaxIdx = "+maxIdx);
				SimpleMatrix matN = matNT.copy();

				if(inSet.contains(maxIdx))//goes out
				{
					System.out.println("MaxIdx was in, goes out");
					inSet.remove(maxIdx);
					
					for(int i=0;i<count+1;i++)
					{
						for(int j=0;j<count+1;j++)
						{
							matN.set(i, j, matNT.get(i, j)-matNT.get(i, maxIdx)*matNT.get(maxIdx, j)/matNT.get(maxIdx, maxIdx));
						}
					}
				}
				else//goes in
				{
					System.out.println("MaxIdx was out, goes in");
					SimpleMatrix matCol = matM.extractVector(false, maxIdx);
					SimpleMatrix matB = matNT.mult(matCol);
					System.out.println("matB");
					matB.print();
					double b = (matB.transpose().mult(matCol)).get(0, 0);
					double c = matM.get(maxIdx, maxIdx) - b;
					System.out.println("b = "+b+" ,c= "+c);
					
					matN.set(maxIdx, maxIdx, 1/c);
					
					for(int i=0;i<count+1;i++)
					{
						if(i == maxIdx)
							continue;
						System.out.println("position i: "+i+", Bi = "+matB.get(i, 0));
						matN.set(maxIdx, i, 0-matB.get(i, 0)/c);
						matN.set(i, maxIdx, 0-matB.get(i, 0)/c);
						
						for(int j=0;j<count+1;j++)
						{
							if(j == maxIdx)
								continue;
							System.out.println("position i,j: "+i+","+j+", Bi = "+matB.get(i, 0)+"Bj = "+matB.get(j, 0));
							System.out.println("matNT(i,j) = "+matNT.get(i, j));
							System.out.println("result = "+(matNT.get(i, j)+matB.get(i, 0)*matB.get(j, 0)/c));
							
							matN.set(i, j, matNT.get(i, j)+matB.get(i, 0)*matB.get(j, 0)/c);
						}
					}
					inSet.add(maxIdx);
				}

				lambdaE = max;
				lambdaLimit = max;
				if(lambdaE <0)
					lambdaE = 0;
				if(max <= 0)
					fini = true;

				matNT = matN;
				System.out.println("new matNT");
				matNT.print();
				
				matTT = matNT.mult(matR);
				System.out.println("matTT matrix:");
				matTT.print();
				
				matUT = matNT.mult(matS);
				
				System.out.println("matUT matrix:");
				matUT.print();
				
				SimpleMatrix matProp = matTT.plus(matUT.scale(lambdaE));
				System.out.println("matProp matrix:");
				matProp.print();
				
				prop = new double[count];
				for(int i=0;i<count;i++)
				{
					prop[i] = matProp.get(i, 0);
				}
				
				proportion.add(prop);	
				lastId = maxIdx;
			}
			
			return proportion;
		}
		
		SimpleMatrix setMatrixCross(int row, int col, double val, SimpleMatrix mat)
		{
			SimpleMatrix result = mat.copy();
			//System.out.println("set matrix cross - result:");
			//result.print();
			
			int rowCount = mat.numRows();
			int colCount = mat.numCols();
			for(int i=0;i<rowCount;i++)
				result.set(i, col, 0);
			for(int i=0;i<colCount;i++)
				result.set(row, i, 0);
			
			result.set(row, col, val);
			
			return result;
		}
		
	}
	
	public void execPortfolioOptimization(ArrayList<StockEntry> stocks) throws Exception
	{
		this.optimiser = new OptiAnalyse();
		this.optimiser.progIndicator = this.progIndicator;
		this.optimiser.opti = this;
		this.optimiser.stocks = stocks;
		
		Thread th = new Thread(this.optimiser);
		th.start();
	}
	
	public void displayOptiResult(List<StockEntry> stocks, double[] esperance,double []variance, ArrayList<double[]> proportions)
	{
		this.frameStock.loadOptiDataToTable(stocks, esperance,variance, proportions);
	}
}
