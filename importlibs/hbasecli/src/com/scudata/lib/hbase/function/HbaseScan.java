package com.scudata.lib.hbase.function;

import java.io.IOException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;
import com.scudata.common.Logger;

public class HbaseScan extends HbaseQuery {
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		if (m_tableInfo == null) {
			m_tableInfo = new TableInfo(OprationType.OPRATION_SCAN);
		}
		return this;
	}
	
	public Object calculate(Context ctx) {
		try {
			return super.calculate(ctx);
		} catch (NumberFormatException e) {
			Logger.error(e.getMessage());
		} 
		
		return null;
	}

	// 全表扫描param(conn, tableName)
	public Object doHbaseQuery( Object obj[]){
		String option = this.getOption();
		if (option!=null && option.equals("c")){ //for 游标
			return hbaseCursor(obj);
		}else{
			return hbaseQuery( obj);
		}		
	}
	
	// 投影字段
	public Object doHbaseQuery( TableInfo tb ) throws IOException{
		String option = this.getOption();
		if (option!=null && option.equals("c")){ //for 游标
			return hbaseCursor(tb);
		}else{
			return hbaseQuery( tb);
		}		
	}
	
	public Table hbaseQuery( Object obj[])
	{
		Table tb = null;
	   	 try {
	   		super.hbaseQuery(obj);
			
	   		Scan scan = new Scan();
			HbaseDriverCli client = (HbaseDriverCli)obj[0];
            org.apache.hadoop.hbase.client.Table table = client.m_conn.getTable(TableName.valueOf((String)obj[1]));
            ResultScanner scanner=table.getScanner(scan);
            tb = toTable(scanner);

            scanner.close();
            scanner = null;
            table.close();
        } catch (IOException e) {            
            Logger.error(e.getMessage());
        }
	   	 
	   	return tb;
	}
	
	public Table hbaseQuery( TableInfo tb ) throws IOException
    {        
		Table tbl = null;
    	 try {
    		 Scan scan = hbaseScanInfo(tb);
    		 HbaseDriverCli client = (HbaseDriverCli)tb.m_connect;
    		 org.apache.hadoop.hbase.client.Table table = client.m_conn.getTable(TableName.valueOf(tb.m_tableName));
             ResultScanner scanner=table.getScanner(scan);//返回迭代器
             tbl = toTable(scanner, tb, 0);
             scanner.close();
             scanner = null;
             table.close();
             scanner = null;
         } catch (IOException e) {            
             Logger.error(e.getMessage());
         }
    	 
    	 return tbl;
    }
  
  	// 全表扫描param(conn, tableName)
	public Object hbaseCursor( Object obj[])
	{
	   	 try {
	   		super.hbaseQuery(obj);
	   		
	   		HbaseDriverCli client = (HbaseDriverCli)obj[0];
	   		Scan scan = new Scan();
	   		m_tableInfo.setTableName((String)obj[1]);
		   	return client.queryRange(m_ctx, scan, m_tableInfo);      
        } catch (Exception e) {            
            Logger.error(e.getMessage());
        }
	   	 
	   	 return null;
	}
	
	public Object hbaseCursor( TableInfo tb ) throws IOException
    {        
		 try {
			 Scan scan = hbaseScanInfo(tb);
			 HbaseDriverCli client = (HbaseDriverCli)tb.m_connect;
			 return client.queryRange(m_ctx, scan, tb);            
		 } catch (Exception e) {            
		     Logger.error(e.getMessage());
		 }
    	 
		return null;
    }
}
