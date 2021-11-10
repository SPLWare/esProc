package com.raqsoft.lib.hive.function;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Schema;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.processors.CommandProcessorResponse;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.lib.hive.function.Utils;

public class HiveBase {
	public Driver driver;
	private String sql;
	private String colNames[];
	private List<Object> result;
	private CommandProcessorResponse res;

	public HiveBase(Driver drv){
		driver = drv;
		result = new ArrayList<Object>();
	}
	
	public Table selectData( String sql) {
		Table tb = null;
		try {
			do{
				if (!queryData(sql)){
					break;
				}
				if(!driver.getResults(result)){
					System.out.println("queryData driver getResult false");
					break;
				}			
				//doPrint(result);
				
				if (result.size()==0){
					System.out.println("no data");
					break;
				}
				tb = toTable(Utils.resultsConvertDList(result), colNames);
				if (tb==null){
					System.out.println("no data");
					break;
				}			
				//test for raq.table 
				if (1==2){
					Utils.testPrintTable(tb);
				}
			}while(false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tb;
	}
	
	public boolean queryData( String sql) {
		boolean bRet = false;
		do{
			if (!execSql(sql)) {
				break;
			}
			result.clear();
			// table colName info
			colNames = getColNames(driver.getSchema());			
			
			//System.out.println(" resultSize" + result.size());
			bRet = true;		
		}while(false);
		
		return bRet;
	}

	public Table describeTables( String tableName) {
		Table tb = null;
		sql = "describe " + tableName;
		try {
			if (!execSql(sql)) {
				return tb;
			}
			result.clear();
			driver.getResults(result);
			//doPrint(result);	
			colNames = new String[]{"col_name","data_type","comment"};
			tb = toTable(Utils.resultsConvertDList(result), colNames);
			if (1==2){
				Utils.testPrintTable(tb);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return tb;
	}
	
	//desc formatted 
	public Table descFormattedTables( String tableName) {
		Table tb = null;
		sql = "describe formatted " + tableName;
		try {
			if (!execSql(sql)) {
				return tb;
			}
			result.clear();
			driver.getResults(result);
			// doPrint(result);	
			colNames = new String[]{"col_name","data_type","comment"};
			tb = toTable(Utils.resultsConvertDList(result), colNames);
			if (1==1){
				Utils.testPrintTable(tb);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tb;
	}
	public void showTables( String tableName) {
		sql = "show tables '" + tableName + "'";
		try {
			if (!execSql(sql)) {
				return;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	// ///////////////////
	public boolean execSql( String sql) {
		boolean bRet = false;
		try {
			// sql = "select * from " + tableName;
			//System.out.println("Running:" + sql);
			if (driver==null){
				throw new RQException("hive driver is null");
			}
			//String currentUser = System.getProperty("user.name");
		    //System.out.println("Current user is " + currentUser);
			long nStart = System.currentTimeMillis();
			res = driver.run(sql);
			if (res.getResponseCode() != 0) {
				System.out.println("run sql:"+sql);
				System.out.println("run error:"+res.getErrorMessage());				
			}else{
				bRet = true;
			}
			System.out.println("time = "+( System.currentTimeMillis()-nStart));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return bRet; 
	}
	
	static Table toTable(List<List<Object>> rows, String[] colNames) {
		int len = rows.size();
		Table table = new Table(colNames, len);

		for (List<Object> row : rows) {
			table.newLast(row.toArray());
		}

		return table;
	}

	// 获取表的列名
	private String[] getColNames(Schema schema) {
		List<FieldSchema> fieldSchema = schema.getFieldSchemas();
		String cols[] = new String[fieldSchema.size()];
		int i = 0;
		for (FieldSchema fs : fieldSchema) {
			cols[i] = fs.getName();
			i++;
		}

		return cols;
	}
	
	public int skipOver(long n){
		int count = 0;
		try {
			if (driver == null || n == 0) return 0;
			result.clear();
			int oldRow = driver.getMaxRows();
			driver.setMaxRows((int)n);
			driver.getResults(result);
			count = result.size();
			driver.setMaxRows(oldRow);
			result.clear();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
		
		return count;
	}
	
	public Table getTable(long n) {
		Table tb = null;
		if (driver == null || n < 1) return tb;
		
		try {
			long nCnt = n;
			if (n > ICursor.INITSIZE) {
				nCnt = ICursor.INITSIZE;
			}
			
			//System.out.println("CursorGetSize = " + nCnt);
			result.clear();
			int oldRow = driver.getMaxRows();
			driver.setMaxRows((int)nCnt);
			driver.getResults(result);
			driver.setMaxRows(oldRow);
			if (result.size() == 0) {
				return null;
			}
			
			tb = toTable(Utils.resultsConvertDList(result), colNames);			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return tb;
	}
	
	
}
