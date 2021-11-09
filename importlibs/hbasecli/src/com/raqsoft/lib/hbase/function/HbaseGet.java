package com.raqsoft.lib.hbase.function;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;

public class HbaseGet extends HbaseQuery {
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		if (m_tableInfo == null) {
			m_tableInfo = new TableInfo(OprationType.OPRATION_GET);
		}
		return this;
	}

	public Object calculate(Context ctx) {
		try {
			return super.calculate(ctx);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} 
		
		return null;
	}
	
	/*hbase_or(过滤器1,....)
	3、hbase_get(client,表名,列1:类型:别名,列2:类型:别名,...;
	filter:f,version:i,timeRange:[t1,t2],timeStamp:t)?
	*/
	public void hbaseGet(Connection hConn, String tableName, String family, String column, 
			String rowKey, Filter filter, int maxVersions, long minStamp, long maxStamp,long timestamp )
    {        
		//System.out.println("rowkey100 = " + rowKey);
    	Get get = new Get(rowKey.getBytes());
    	if (filter!=null){
    		get.setFilter(filter);
    	}
    	boolean bAddFamily = false;
    	if (column!=null && !column.isEmpty()){
    		if (family!=null && !family.isEmpty()){
    			bAddFamily = true;
    			get.addColumn(family.getBytes(), column.getBytes()); 
        	}
    	}
    	if (!bAddFamily)
    	{
    		if (family!=null && !family.isEmpty()){
    			get.addFamily(family.getBytes());
    		}
    	}

    	 try {
    		 org.apache.hadoop.hbase.client.Table table = hConn.getTable(TableName.valueOf(tableName));
             Result res=table.get(get);//返回迭代器
            
             ImUtils.format(res);
             System.out.println();                         
         } catch (IOException e) {            
             e.printStackTrace();
         }
    }

	//param(conn, tableName, rowkey)
	public Table queryByRowkeys( Object obj[]) throws IOException {
		Table tb = null;
		try {
			//System.out.println("paramSize = " + obj.length);
			if (obj.length != 3) {
				throw new RQException(
						"hbaseGet hbaseQuery tableName or rowKey is not right");
			}
			
			HbaseDriverCli client = (HbaseDriverCli)obj[0];
			if (client == null) {
				throw new RQException("hbaseGet hbaseQuery HbaseDriverCli is null");
			}
			Connection hConn = client.m_conn;
			if (hConn == null) {
				throw new RQException("hbaseGet hbaseQuery hConn is null");
			}
			
			org.apache.hadoop.hbase.client.Table table = hConn.getTable(TableName.valueOf((String) obj[1]));
			if ( obj[2] instanceof String){
				String rowKey = (String) obj[2];			
				Get get = new Get(rowKey.getBytes());				
				
				Result res=table.get(get);
				tb = toTable(new Result[]{res});
			}else if(obj[2] instanceof Sequence){
				Sequence seq = (Sequence) obj[2];
				List<Get> gets=new ArrayList<Get>();
				for(int i=0; i<seq.length(); i++){
					String rowkey = (String)seq.get(i+1);
					Get get=new Get(rowkey.getBytes());
	                gets.add(get);
				}
				Result res[]=table.get(gets);
				tb = toTable(res);
			}
			table.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return tb;
	}
	
	// 输出result结果
	public Table toTable(Result[] results) {		
		if (results==null || results.length<1) return null;
		
		String[] colNames = null;
  		
  		Table table = null;
  		boolean bFirst = true;
  		Object[] objs=null;
  		List<String> list = new ArrayList<String>();  		
  		List<String> vlist = new ArrayList<String>();
  		
  		String family, column, fullName;
  		for(Result res: results){
  			String rowkey=Bytes.toString(res.getRow());
  			
  			if (bFirst){
  				bFirst = false;	
  				getColumnArray(res, list, vlist);
  				colNames = vlist.toArray(new String[vlist.size()]);
  				table = new Table(colNames);
  			 }
  			
   			objs = new Object[colNames.length];  
  			objs[0] = rowkey;
  			Record r = table.newLast(objs);
  			List<Cell> cells = res.listCells();
  	        for (Cell c : cells) {
  				//System.out.println("val = "+Bytes.toString(kv.getValue()));
  				family= Bytes.toString(CellUtil.cloneFamily(c));
  				column= Bytes.toString(CellUtil.cloneQualifier(c));
  				if (column.compareTo("_0")==0){
  					continue;
  				}
  				
  				fullName = family+"."+column;
  				if (vlist.indexOf(fullName)>-1){
  	            	r.set(fullName, (Object)Bytes.toString(CellUtil.cloneValue(c)));
  	            }
  	        }//endfor
  		}//endfor
  		if (list.size()>0 && table!=null ){
	  		String[] cols = list.toArray(new String[list.size()]);
	  		table.rename(colNames, cols);
  		}
  		return table;
	}

	public Object doHbaseQuery(  Object obj[] ) throws IOException
	{  
		return queryByRowkeys(obj);
	}
	
	//get()查询
	//@SuppressWarnings("deprecation")
	public Object doHbaseQuery( TableInfo tb ) throws IOException
    {        
		// check param
		if (tb == null){
			throw new RQException("hbaseGet param tableInfo is null" );
		}
		
		if (tb.m_connect == null){
			throw new RQException("hbaseGet param connect is null" );
		}
		
		if (tb.m_rowkey==null || tb.m_rowkey.isEmpty()){
			throw new RQException("hbaseGet param rowkey is empty" );
		}
		
		if (tb.m_tableName == null || tb.m_tableName == ""){
			throw new RQException("hbaseGet param tableName is empty" );
		}
		int size = tb.m_family.size();
		if (size==0){
			throw new RQException("hbaseGet param family is empty" );
		}
		
		if (tb.m_column.size()!=size){
			throw new RQException("hbaseGet param column is not right" );
		}
		if (tb.m_columnType.size()!=size){
			throw new RQException("hbaseGet param columnType is not right" );
		}
		if (tb.m_columnAlias.size()!=size){
			throw new RQException("hbaseGet param columnAlias is not right" );
		}
		//System.out.println("rowkey200 = " + tb.m_rowkey);
		Get get = new Get(tb.m_rowkey.getBytes());
    	if (tb.m_filter!=null){
    		get.setFilter(tb.m_filter);
    	}
    
    	if (tb.m_version>0){
    		get.setMaxVersions(tb.m_version);
    	}
    	
    	if (tb.m_minTimeStamp>0 && tb.m_maxTimeStamp>0){
    		get.setTimeRange(tb.m_minTimeStamp, tb.m_maxTimeStamp);
    	}
    	if ( tb.m_timeStamp>0){
    		get.setTimeStamp(tb.m_timeStamp);
    	}
    	
    	for(int i=0; i<size; i++){
    		get.addColumn(tb.m_family.get(i).getBytes(), tb.m_column.get(i).getBytes()); 
    	}
    	
    	Table tbl = null;
    	try {
    		 HbaseDriverCli client = (HbaseDriverCli)tb.m_connect;
    		 org.apache.hadoop.hbase.client.Table table = client.m_conn.getTable(TableName.valueOf(tb.m_tableName));
             Result res=table.get(get);//返回迭代器
             
             tbl = toTable(res, tb);                     
         } catch (IOException e) {            
             e.printStackTrace();
         }
    	 
    	 return tbl;
    }
	
	//查询结果转换成序表
	static Table toTable(Result res, TableInfo tb) {
  		if ( res == null) {
  			throw new RQException("toTable param res is not null" );
  		}
  		
  		if ( tb == null) {
  			throw new RQException("toTable param TableInfo is not null" );
  		}
  		
  		int colSize = 0;
  		Object[] objs=null;
  		String family, column,fullName;
  		
  		// reset column;
  		colSize = tb.m_columnAlias.size()+1; 	
  		Table table = createTable(tb);
  		if (table==null){
  			throw new RQException("toTable param create table false" );
  		}
  		if (res.size()==0){
  			return table;
  		}
  		
      	String sAliasKey = "";  		
		String rowkey=Bytes.toString(res.getRow());
		
		objs = new Object[colSize];  
		objs[0] = rowkey;
		Record r = table.newLast(objs);

		List<Cell> cells = res.listCells();
        for (Cell c : cells) {
			//System.out.println("val = "+Bytes.toString(kv.getValue()));
			family= Bytes.toString(CellUtil.cloneFamily(c));
			column= Bytes.toString(CellUtil.cloneQualifier(c));
			if (column.compareTo("_0")==0){
				continue;
			}
			
            fullName = family+"."+column;
            sAliasKey = tb.m_columnAlias.get(fullName);
            objs[0] = ImUtils.getDataType(tb, fullName, Bytes.toString(CellUtil.cloneValue(c)));
            r.set(sAliasKey, objs[0]);
        }
  		
  		return table;
  	}
	
	//根据tableInfo创建序表
	private static Table createTable(TableInfo tb) {
		if (tb == null){
			throw new RQException("createTable tableInfo is null");
		}
		
		String[] colNames = null;
		int colSize = tb.m_columnAlias.size() + 1;
		colNames = new String[colSize];
		colNames[0] = "rowkey";
		int i = 1;
		for (String val : tb.m_columnAlias.values()) {
			colNames[i++] = val;
		}
	
		return new Table(colNames, colSize);
	}
}
