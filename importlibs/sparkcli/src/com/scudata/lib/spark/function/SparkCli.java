package com.scudata.lib.spark.function;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.scudata.common.Logger;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IResource;
import com.scudata.dm.Record;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;

//dfsUrl = "hdfs://master:9000/user/hive/warehouse";
//thriftUrl = "thrift://master:9083";
//hadoopMasterHost = "master";

public class SparkCli implements IResource{
	private SparkSession spark;
	private Dataset<Row> result;
	private ClassLoader  m_classLoader=null;
	Iterator<Row> iterator = null;
	JavaSparkContext sc = null;

	public SparkCli(Context ctx, String hdfsUrl, String thriftUrl, String dbName) {
		init(hdfsUrl, thriftUrl, dbName);	
		ctx.addResource(this);
	}

	// 关闭连接释放资源
	public void close() {
		System.out.println("SparkDriverCli quit....");
		if (m_classLoader!=null){
			Thread.currentThread().setContextClassLoader(m_classLoader);
			m_classLoader=null;
		}
		spark.stop();
	}
	// 初始化Spark
	private void init(String hdfsUrl, String thriftUrl, String dbName)  {
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
			
			{
				String envPath = System.getProperty("java.library.path"); 
				String path = System.getProperty("user.dir");
				envPath = path + ";" + envPath;
				path = path.replace("\\bin", ""); 
				System.setProperty("hadoop.home.dir", path);				
			}
			
			SparkConf conf = new SparkConf().setAppName("Spark Raqsoft").setMaster("local");

			conf.set("fs.default.name", hdfsUrl);
			conf.set("hive.metastore.local", "false");
			conf.set("hive.metastore.uris", thriftUrl);
			conf.set("yarn.nodemanager.hostname", masterName);
			conf.set("yarn.resourcemanager.hostname", masterName);
			
			m_classLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader loader = SparkCli.class.getClassLoader();
			Thread.currentThread().setContextClassLoader(loader);
			
			spark = SparkSession
		      .builder()
		      .appName("Java Spark SQL basic example")
		      .config(conf)
		      .config("spark.some.config.option", "some-value")
		      .enableHiveSupport()
		      .getOrCreate();

			String cmd = "use "+dbName;
			spark.sql(cmd);
			System.out.println("Init Spark Success");
		} catch (Exception e) {
			Logger.error(e.getMessage());
		} 
	}

	/**
	 * spark List<Object>转为润乾序表
	 * 
	 * @param sql
	 *  sql 语句;
	 * @return Table
	 */
	public Table query(String sql) {
		Table tb = null;

		try {
			// table records
			Logger.info("Running:" + sql);
			result = spark.sql(sql);
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
	static Table toTable(Iterator<Row> itr, String []colNames, int len) {
		if (len<1 || itr == null) return null;
		Table table = new Table(colNames, len);
		
		int nCount = 0;
		String str = "";
		
		while (itr.hasNext() && nCount<len) {
			Row row = (Row)itr.next();
			str = row.toString();
			str = str.substring(1,str.length()-1); 
			table.newLast(str.split(","));
			nCount++;
		}
		
		return table;
	}
		
	public ICursor cursorQuery(String sql, Context ctx) {
		if (execSql(sql)){		
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
	
	//是否合法的sql语句.
	public static boolean isLegalSql(String strSql)
	{
	  String span=strSql.toUpperCase();//测试用sql语句
	  System.out.println(span);
	  String column="(\\w+\\s*(\\w+\\s*){0,1})";//一列的正则表达式 匹配如 product p
	  String columns=column+"(,\\s*"+column+")*"; //多列正则表达式 匹配如 product p,category c,warehouse w
	  String ownerenable="((\\w+\\.){0,1}\\w+\\s*(\\w+\\s*){0,1})";//一列的正则表达式 匹配如 a.product p
	  String ownerenables=ownerenable+"(,\\s*"+ownerenable+")*";//多列正则表达式 匹配如 a.product p,a.category c,b.warehouse w
	  String from="FROM\\s+"+columns;
	  String condition="(\\w+\\.){0,1}\\w+\\s*(=|LIKE|IS)\\s*'?(\\w+\\.){0,1}[\\w%]+'?";//条件的正则表达式 匹配如 a=b 或 a is b..
	  String conditions=condition+"(\\s+(AND|OR)\\s*"+condition+"\\s*)*";//多个条件 匹配如 a=b and c like 'r%' or d is null 
	  String where="(WHERE\\s+"+conditions+"){0,1}";
	  String pattern="SELECT\\s+(\\*|"+ownerenables+"\\s+"+from+")\\s+"+where+"\\s*"; //匹配最终sql的正则表达式
	  System.out.println(pattern);//输出正则表达式
	  
	  boolean bRet = span.matches(pattern);//是否比配
	  
	  return bRet;
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
}