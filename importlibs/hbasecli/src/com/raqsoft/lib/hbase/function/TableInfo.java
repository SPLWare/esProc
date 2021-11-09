package com.raqsoft.lib.hbase.function;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.filter.Filter;

/*hbase_scan(client,表名,列1:类型:别名,列2:类型:别名,...;rowPrefix:x,
filter:f,limit:l,version:i,startRow:x,stopRow:x,timeRange:[t1,t2],timeStamp:t)
*/
enum OprationType {
	OPRATION_GET, 
	OPRATION_SCAN
};

public class TableInfo extends Object{
	public OprationType m_oprationType;		//get:1, scan:2, 
	public String		m_rowkey;
	public Object		m_connect;
	public String 		m_tableName;
	public List<String> m_family;
	public List<String>	m_column;
	public LinkedHashMap<String, String> m_columnType;
	//key: column, value: alias,若alias为空，则value=key
	public LinkedHashMap<String, String> m_columnAlias; 
	public String		m_rowPrefix;
	public Filter		m_filter;
	public int			m_limit;
	public int			m_version;
	public String		m_startRow;
	public String		m_stopRow;
	public long			m_minTimeStamp;
	public long			m_maxTimeStamp;
	public long			m_timeStamp;	
	
	public TableInfo(OprationType oprationType){
		m_oprationType = oprationType;
		init();
	}
	

	public void setOprationType(OprationType oprationType){
		m_oprationType = oprationType;
	}

	public void setRowkey(String key){
		m_rowkey = key;
	}
	
	public void setConnect(Object f){
		m_connect = f;
	}
	public void setFilter(Filter f){
		m_filter = f;
	}
	
	public void setTableName(String s){
		m_tableName = s;
	}
	public void setFamily(String s){
		m_family.add(s);
	}
	
	public void setColumn(String s){
		m_column.add(s);
	}

	public void setColumnType(String key, String val){
		m_columnType.put(key, val);
	}
	
	public void setColumnAlias(String key, String val){
		m_columnAlias.put(key, val);
	}
	
	public void setRowPrefixFilter(String s){
		m_rowPrefix = s;
	}
	
	public void setLimit(int n){
		m_limit = n;
	}
	
	public void setVersion(int n){
		m_version = n;
	}
	public void setStartRow(String s){
		m_startRow = s;
	}
	public void setStopRow(String s){
		m_stopRow = s;
	}
	public void setMinTimeStamp(long l){
		m_minTimeStamp = l;
	}
	public void setMaxTimeStamp(long l){
		m_maxTimeStamp = l;
	}
	public void setTimeStamp(long l){
		m_timeStamp = l;
	}
	
	private void init()
	{
		m_rowkey = "";
		m_connect = null;
		m_filter = null;
		m_tableName = "";
		m_family = new ArrayList<String>();
		m_column = new ArrayList<String>();
		m_columnType= new LinkedHashMap<String,String>();
		m_columnAlias= new LinkedHashMap<String,String>();
		m_rowPrefix= "";
		m_limit = 0;
		m_version = 0;
		m_startRow= "";
		m_stopRow= "";
		m_minTimeStamp = 0;
		m_maxTimeStamp = 0;
		m_timeStamp = 0;	
	}
	
	public void reset()
	{
		m_rowkey = "";
		m_connect = null;
		m_filter = null;
		m_tableName = "";
		m_family.clear();
		m_column.clear();
		m_columnType.clear(); 
		m_columnAlias.clear();
		m_rowPrefix= "";
		m_limit = 0;
		m_version = 0;
		m_startRow= "";
		m_stopRow= "";
		m_minTimeStamp = 0;
		m_maxTimeStamp = 0;
		m_timeStamp = 0;	
	}
	
	public void printTest(String sinfo)
	{
		System.out.println(sinfo);
		System.out.println("m_rowkey = " + m_rowkey);
		System.out.println("connect = " + m_connect);
		System.out.println("filter = " +  m_filter);
		System.out.println("m_tableName = "+ m_tableName);
		System.out.println("m_family = "+m_family);
		System.out.println("m_column = " +m_column);
		System.out.println("m_columnType = " +m_columnType);
//		for(String s:m_column){
//			System.out.println("m_column = "+ s); 
//		}
//		for(String s:m_columnType){
//			System.out.println("m_columnType = "+ s); 
//		}
		
		for (Map.Entry<String, String> entry : m_columnAlias.entrySet()) {  
		    System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());  
		} 
		System.out.println("m_columnAlias = "+m_columnAlias);
		System.out.println("m_rowPrefix = " +m_rowPrefix);
		System.out.println("m_limit = "+m_limit);
		System.out.println("m_version = "+m_version);
		System.out.println("m_startRow = "+m_startRow);
		System.out.println("m_stopRow= "+ m_stopRow);
		System.out.println("m_minTimeStamp = "+m_minTimeStamp);
		System.out.println("m_maxTimeStamp = "+m_maxTimeStamp);
		System.out.println("m_timeStamp = "+m_timeStamp);	
	}
}
