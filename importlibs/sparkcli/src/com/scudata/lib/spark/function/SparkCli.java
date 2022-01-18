package com.scudata.lib.spark.function;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RuntimeConfig;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;

import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IResource;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import scala.collection.mutable.WrappedArray;

//dfsUrl = "hdfs://master:9000/user/hive/warehouse";
//thriftUrl = "thrift://master:9083";
//hadoopMasterHost = "master";

public class SparkCli implements IResource{
	private SparkSession spark;
	private Dataset<Row> result = null;
	private ClassLoader  m_classLoader=null;
	Iterator<Row> iterator = null;
	JavaSparkContext sc = null;

	// local
	public SparkCli(Context ctx) {
		initEnv();
		SparkConf conf = new SparkConf().setAppName("Spark Raqsoft").setMaster("local");
		sc = new JavaSparkContext(conf);
		
		spark = SparkSession
	      .builder()
	      .master("local")
	      .appName("Java Spark SQL")
	      .getOrCreate();
		ctx.addResource(this);
	}
	
	// remote hdfs
	public SparkCli(Context ctx, String hdfsUrl)
	{
		initEnv();
		Matcher m[] = new Matcher[1];
		if (!isMatch(hdfsUrl, "hdfs:\\/\\/(.*?):(\\d+)", m)){
			Logger.debug("url:"+hdfsUrl + " is error expression");
			return;
		}
		SparkConf conf = new SparkConf().setAppName("Spark Raqsoft").setMaster("local");
		conf.set("fs.default.name", hdfsUrl);
		sc = new JavaSparkContext(conf);
		
		spark = SparkSession
	      .builder()
	      .master("local")
	      .config(conf)
	      .appName("Java Spark SQL")
	      .getOrCreate();
		//String url = spark.conf().get("fs.default.name");
		
		ctx.addResource(this);
	}
	
	// remote
	public SparkCli(Context ctx, String hdfsUrl, String thriftUrl, String dbName) {
		try {
			Matcher m[] = new Matcher[1];
			if (!isMatch(hdfsUrl, "hdfs:\\/\\/(.*?):(\\d+)", m)){
				Logger.debug("url:"+hdfsUrl + " is error expression");
				return;
			}
			String masterName = "master";
			if ( 2==m[0].groupCount()){
				masterName = m[0].group(1);
			}
			
			if (!isMatch(thriftUrl, "thrift:\\/\\/(.*?):(\\d+)", m)){
				Logger.debug("url:"+thriftUrl + " is error expression");
				return;
			}
			
			initEnv();
			
			SparkConf conf = new SparkConf().setAppName("Spark Raqsoft").setMaster("local");
			conf.set("fs.default.name", hdfsUrl);
			conf.set("hive.metastore.local", "false");
			conf.set("hive.metastore.uris", thriftUrl);
			conf.set("yarn.nodemanager.hostname", masterName);
			conf.set("yarn.resourcemanager.hostname", masterName);
			
			spark = SparkSession
		      .builder()
		      .appName("Java Spark SQL")
		      .config(conf)
		      .enableHiveSupport()
		      .getOrCreate();

			String cmd = "use "+dbName;
			spark.sql(cmd);
			System.out.println("Init Spark Success");
			ctx.addResource(this);
		} catch (Exception e) {
			Logger.error(e.getMessage());
		} 		
	}
	
	private void initEnv(){
		String path = "";
		String home=System.getProperty("start.home"); 
		if (home!=null){
			path = home+File.separator+"bin"; 
			System.setProperty("hadoop.home.dir", path);
		}else{
			path = System.getProperty("user.dir");
			path = path.replace("\\bin", ""); 
			System.setProperty("hadoop.home.dir", path);	
		}
		
		m_classLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader loader = SparkCli.class.getClassLoader();
		Thread.currentThread().setContextClassLoader(loader);
	}
	
	// 关闭连接释放资源
	public void close() {
		if (m_classLoader!=null){
			Thread.currentThread().setContextClassLoader(m_classLoader);
			m_classLoader=null;
		}
		spark.stop();
	}
	
	/**
	 * spark List<Object>转为润乾序表
	 * 
	 * @param sql
	 *  sql 语句;
	 * @return Table
	 */
	public Table exec(String strVal) {
		Table tb = null;

		try {
			if (strVal==null || strVal.isEmpty()) return null;
			
			// table records for sql
			Logger.info("Running:" + strVal);
			result = spark.sql(strVal);
			if (result.count()<1){
				return tb;
			}
		
			iterator = result.toLocalIterator();
			tb = toTable(iterator, result.columns(), (int)result.count());
		} catch (Exception e) {
			Logger.error(e.getMessage());
		} 

		return tb;
	}
	
	public Table read(String strVal, Map<String,String> map) {
		Table tbl = null;
		if (execRead(strVal, map)){
			tbl = toTable(iterator, result.columns(), (int)result.count());
		}
		return tbl;
	}
	
	public Table readSequenceFile(String strVal, Map<String,String> map) {
		Table tbl = null;
		try {
			String sKey = "org.apache.hadoop.io.Text";
			String sVal = "org.apache.hadoop.io.IntWritable";
			if (map.containsKey("k")) {
				sKey = map.get("k");
			}

			if (map.containsKey("v")) {
				sVal = map.get("k");
			}
			
			tbl = new Table(new String[]{"Key","Val"});
			Class clzKey = Class.forName(sKey);
			Class clzVal = Class.forName(sVal);
			JavaPairRDD input = sc.sequenceFile(strVal, clzKey, clzVal, 1);
			input.foreach(f-> {
             	System.out.println(f.toString());
             	}
             );
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		return tbl;
	}
	
	
	/**
	 * spark读取数据
	 * 
	 * @param sql
	 *  sql 语句;
	 * @return Table
	 */
	public boolean execRead(String strVal, Map<String,String> map) {
		boolean bRet = false;
		DataFrameReader[] reader = new DataFrameReader[1];
		try {
			if (strVal==null || strVal.isEmpty()) {
				return bRet;
			}
			
			reader[0] = spark.read();
			map.forEach((key, value) -> {
				reader[0].option(key, value);
	        });
			//for file
			boolean bNeedRun = true;
			String sfile = strVal.toLowerCase();
			String format=null;
			if (sfile.endsWith(".csv")){ //缺省分隔符为";"
				format="csv";
				bNeedRun = false;
				if (!map.containsKey("sep")){
					reader[0].option("sep", ";");
				}
				if (!map.containsKey("header")){
					reader[0].option("header", "true");
				}
				result = reader[0].format(format).load(strVal);	
			}else if(sfile.endsWith(".json")){
				format="json";
			}else if(sfile.endsWith(".avro")){
				format="com.databricks.spark.avro";
			}else if(sfile.endsWith(".txt")){//用text()解析需要转换,缺省分隔符为","
				format="csv";
				bNeedRun = false;
				if (!map.containsKey("sep")){
					reader[0].option("sep", ",");
				}
				result = reader[0].format(format).load(strVal);
			}else if(sfile.endsWith(".orc")){
				format="orc";
			}else{
				bNeedRun = false;
				//format: parquet
				result = reader[0].load(strVal);
			}
			
			if (bNeedRun){
				result = reader[0].format(format).load(strVal);	
			}
		
			iterator = result.toLocalIterator();
			bRet = true;
		} catch (Exception e) {
			Logger.error(e.getMessage());
		} 

		return bRet;
	}
	
	public int skipOver(long n){
		int count = 0;
		try {
			if (spark == null || n == 0 || iterator==null) return 0;
			while (iterator.hasNext() && count<n) {
				iterator.next();
				count++;
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
		} 	
		
		return count;
	}

	public Table getTable(int n) {
		Table tb = null;
		if (spark == null || n < 1) return tb;
		
		try {
			int nCnt = n;
			if (n > ICursor.INITSIZE) {
				nCnt = ICursor.INITSIZE;
			}
			
			if (result.count() == 0) {
				return null;
			}
			
			tb = toTable(iterator, result.columns(), nCnt);			
		} catch (Exception e) {
			Logger.error(e.getMessage());
		} 
		
		return tb;
	}
	
	public void testPrintTable(Table table){
		if (table == null) return;
		System.out.println("size = " + table.length());
		
		DataStruct ds = table.dataStruct();
		String[] fields = ds.getFieldNames();
		int i = 0;
		// print colNames;
		for(i=0; i<fields.length; i++){
			System.out.print( fields[i]+"\t");
		}
		System.out.println();
		// print tableData
		for(i=0; i<table.length(); i++){
			Record rc = table.getRecord(i+1);
			Object []objs = rc.getFieldValues();
			for (Object o : objs){
				System.out.printf(o + "\t");
			}
			System.out.println();
		}
	}
	
	//将List<List>转换成Talbe, 
	private Table toTable(Iterator<Row> itr, String []colNames, int len) {
		if (len<1 || itr == null) return null;
		Table table = new Table(colNames);
			
		int nCount = 0;
		Object[] lines=null;
		String tmp = "";
		while(itr.hasNext() && nCount<len){
	    	Row row = itr.next();
	    	lines = new Object[colNames.length];
	    	int i = 0;
	    	for(String col:colNames){
	    		Object o = row.getAs(col);
	    		if (o instanceof GenericRowWithSchema){
	    			lines[i++] = doGenericRowWithSchema((GenericRowWithSchema)o);
	    		}else if(o instanceof WrappedArray){
	    			lines[i++] = doWrappedArray((WrappedArray)o);;
	    		}else{
		    		if (o instanceof String){
		    			tmp = o.toString().trim();
		    			if (StringUtils.isNumeric(tmp)) {
		    				lines[i++] = convertStringToNumber(tmp);
		    			}else{
		    				lines[i++] = tmp;
		    			}
		    		}else{
		    			lines[i++] = o;
		    		}
	    		}
	    	}
	    	table.newLast(lines);
			nCount++;
	    }
		
		return table;
	}
	
	private Object doWrappedArray(WrappedArray wa){
		Sequence seq = new Sequence();
		for(int j=0; j<wa.length(); j++){
			Object subLine = wa.apply(j);
			if (subLine instanceof GenericRowWithSchema){
				Object subObj = doGenericRowWithSchema((GenericRowWithSchema)subLine);
				seq.add(subObj);
			}else{
				seq.add(subLine);
			}
		}
		return seq;
	}
		
	private Object doGenericRowWithSchema(GenericRowWithSchema rs){
		String[] fs=rs.schema().fieldNames();
		Object[] vs=rs.values();
		for(int i=0; i<vs.length; i++){
			if (vs[i] instanceof WrappedArray){
				vs[i] = doWrappedArray((WrappedArray)vs[i]);
			}
		}
		Record rcd = new Record(new DataStruct(fs), vs);
		return rcd;
	}
	
	public ICursor cursorQuery(String sql, Context ctx) {
		if (execSql(sql)){		
			return new ImCursor(this, ctx);
		}

		return null;			
	}
	
	public ICursor cursorRead(String sql, Map<String,String> map, Context ctx) {
		if (execRead(sql, map)){		
			return new ImCursor(this, ctx);
		}

		return null;			
	}
	
	private boolean execSql(String sql)
	{
		try {
			iterator = null;
			result = spark.sql(sql);
			if (result.count()<1){
				return false;
			}

			iterator = result.toLocalIterator();
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}

		return true;
	}
	
	// 通过Url获取主机名，port, warehouse
	private boolean isMatch(String strUrl, String regExp, Matcher[] retMatch)
	{
		// 1.通过Url获取主机名，port, warehouse
		//String regex="hdfs:\\/\\/(.*?):(\\d+)(\\/.*)";
		// 2.通过Url获取主机名，port
		//String regex="hdfs:\\/\\/(.*?):(\\d+)";
		if (strUrl==null || strUrl.isEmpty()){
			throw new RQException("spark isMatch strUrl is empty");
		}
		
		if (regExp==null || regExp.isEmpty()){
			throw new RQException("spark isMatch regExp is empty");
		}
		
		Pattern p=Pattern.compile(regExp);
		retMatch[0] = p.matcher(strUrl);
		
		return retMatch[0].matches();
	}
	
	private static Object convertStringToNumber(String str){
		int type = 0;
		try{
			if (str.contains(".")){
				type = 1;
				float v = Float.parseFloat(str);
				if (v==Float.POSITIVE_INFINITY || v==Float.NEGATIVE_INFINITY){
					return Double.parseDouble(str);
				}else{
					return v;
				}
			}else{
				type = 2;
				return Integer.parseInt(str);
			}
		}catch(Exception ex){
			return Long.parseLong(str);
		}
	}
	
	
}