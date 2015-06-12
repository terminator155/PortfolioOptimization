JCC = javac
JFLAGS = -verbose -classpath /usr/local/hadoop/share/hadoop/common/hadoop-common-2.6.0.jar:/usr/local/hadoop/share/hadoop/mapreduce/hadoop-mapreduce-client-core-2.6.0.jar:/usr/local/hadoop/share/hadoop/common/lib/commons-cli-1.2.jar:/home/zlw/workspace/PortfolioOpti/mongo-java-driver-2.13.0.jar:/home/zlw/workspace/PortfolioOpti/mongo-hadoop-core-1.3.3.jar:/home/zlw/workspace/PortfolioOpti/ejml/EJML-core-0.26.jar:/home/zlw/workspace/PortfolioOpti/ejml/EJML-equation-0.26.jar:.

all:compilation jar

compilation: StockEntry.class StockData.class OptiProgressIndicator.class StockCov.class PortfolioFrame.class StockToDBMapper.class StockToDBReducer.class StockStatReducer.class StockStatMapper.class PortfolioOpti.class  

sources:StockData.java StockEntry.java StockCov.java OptiProgressIndicator.java StockCovMapper.java StockCovReducer.java StockStatMapper.java StockStatReducer.java PortfolioOpti.java

StockCov.class:StockCov.java
	$(JCC) $(JFLAGS) StockCov.java

StockEntry.class:StockEntry.java
	$(JCC) $(JFLAGS) StockEntry.java

StockData.class:StockData.java
	$(JCC) $(JFLAGS) StockData.java
	
OptiProgressIndicator.class:OptiProgressIndicator.java
	$(JCC) $(JFLAGS) OptiProgressIndicator.java
	
PortfolioFrame.class:PortfolioFrame.java
	$(JCC) $(JFLAGS) PortfolioFrame.java
	
StockCovMapper.class:StockCovMapper.java StockCov.class
	$(JCC) $(JFLAGS) StockCovMapper.java
	
StockCovReducer.class:StockCovReducer.java StockCov.class
	$(JCC) $(JFLAGS) StockCovReducer.java
	
StockStatMapper.class:StockStatMapper.java
	$(JCC) $(JFLAGS) StockStatMapper.java
	
StockToDBMapper.class:StockToDBMapper.java StockEntry.class
	$(JCC) $(JFLAGS) StockToDBMapper.java
	
StockToDBReducer.class:StockToDBReducer.java StockEntry.class
	$(JCC) $(JFLAGS) StockToDBReducer.java
	
StockStatReducer.class:StockStatReducer.java StockData.class
	$(JCC) $(JFLAGS) StockStatReducer.java

PortfolioOpti.class:PortfolioOpti.java 
	$(JCC) $(JFLAGS) PortfolioOpti.java
	
jar:
	jar cvfe PortfolioOpti.jar PortfolioOpti *.class
	
clean:
	$(RM) *.class
	$(RM) *.jar