package com.raqsoft.lib.hbase.function;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.util.Bytes;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

/**
 * 
 * @author lxch
 * 列名部分解析思路
 * 1. 区分带条件还是不带条件，分号决定
 * 2. 字段部分：list或IParam::array.
 * 3. CHECK_TABLE后为列名部分， 其前为conn,tableName, rowkey
 * 4. 检测是否为family:column结构，若是则标记CHECK_COLUMN
 * 5. 处理family:column后，再处理check_type
 * 6. "family:column"::alias时，check_type对应值为null
 * 7. 循环4,5,6部分。
 */
public class HbaseQuery extends Function {
	enum CheckType {
		CHECK_TABLE, CHECK_COLUMN, CHECK_TYPE, CHECK_ALIAS
	};
	
	protected TableInfo m_tableInfo = null;
	protected CheckType m_chkType;
	protected String m_tmpColumnName; // for alias
	protected Context m_ctx;

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}

		return this;
	}

	public Object calculate(Context ctx) {
		m_ctx = ctx;
		IParam param = this.param;
		try {
			if (param == null || param.getSubSize() < 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("hbase calculate" + mm.getMessage(Integer.toString(param.getSubSize())));
			}
	
			int size = param.getSubSize();

			if (param.getType()==';'){	
				ArrayList<Expression> list1 = new ArrayList<Expression>();
				ArrayList<Expression> list2 = new ArrayList<Expression>();
				param.getSub(0).getAllLeafExpression(list1);
				param.getSub(1).getAllLeafExpression(list2);

				m_tableInfo.reset();
				// 1. for prefix part 
				doTableColumn(ctx, list1);
				
				// 2. for last part 
				doTableParam(ctx, list2);
				
				return doHbaseQuery(m_tableInfo);			
			}else if (param.getType()==','){
				System.out.println("querySize = " + size +" type = " + m_tableInfo.m_oprationType);
				if ( (size==2 && m_tableInfo.m_oprationType==OprationType.OPRATION_SCAN) ||
					 (size==3 && m_tableInfo.m_oprationType==OprationType.OPRATION_GET)	){
					//System.out.println("querySize100 = " + size +" type = " + m_tableInfo.m_oprationType);
					Object obj[] = new Object[size];
					for(int i=0; i<size; i++){
						obj[i] = param.getSub(i).getLeafExpression().calculate(ctx);
					}
					
					return doHbaseQuery(obj);				
				} else {
					//System.out.println("querySize300 = " + size +" type = " + m_tableInfo.m_oprationType);
					doTableColumn(ctx, param);
					return doHbaseQuery(m_tableInfo);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		return null;
	}
	
	public Scan hbaseScanInfo( TableInfo tb ) throws IOException
	{
		// check param
		if (tb == null){
			throw new RQException("hbaseScan param tableInfo is null" );
		}
		
		if (tb.m_connect == null){
			throw new RQException("hbaseScan param connect is null" );
		}
		
		if (tb.m_tableName == null || tb.m_tableName == ""){
			throw new RQException("hbaseScan param tableName is empty" );
		}
		int size = tb.m_family.size();

		if (tb.m_column.size()!=size){
			throw new RQException("hbaseScan param column "+tb.m_column.size()+" is " + size);
		}
		if (tb.m_columnType.size()!=size){
			throw new RQException("hbaseScan param columnType "+tb.m_columnType.size()+" is " + size);
		}
		if (tb.m_columnAlias.size()!=size){
			throw new RQException("hbaseScan param columnAlias "+tb.m_columnType.size()+" is " + size);
		}
		
		Scan scan = new Scan();
    	if (tb.m_filter!=null){
    		scan.setFilter(tb.m_filter);
    	}
    	//System.out.println("scan limit = " + tb.m_limit);
    	if (tb.m_limit>0){
    		System.out.println("scan setCaching limit = " + tb.m_limit);
    		scanSetLimit(scan, tb.m_limit);
    	}
    	
    	if (tb.m_rowPrefix!=null && !tb.m_rowPrefix.isEmpty()){
    		scan.setRowPrefixFilter(tb.m_rowPrefix.getBytes());
    	}
    	
    	if (tb.m_startRow!=null && !tb.m_startRow.isEmpty()){
    		scan.setStartRow(tb.m_startRow.getBytes());
    	}
    	if (tb.m_stopRow!=null && !tb.m_stopRow.isEmpty()){
    		scan.setStopRow(tb.m_stopRow.getBytes());
    	}
    	
    	if (tb.m_version>0){
    		scan.setMaxVersions(tb.m_version);
    	}
    	
    	if (tb.m_minTimeStamp>0 && tb.m_maxTimeStamp>0){
    		scan.setTimeRange(tb.m_minTimeStamp, tb.m_maxTimeStamp);
    	}
    	if ( tb.m_timeStamp>0){
    		scan.setTimeStamp(tb.m_timeStamp);
    	}
    	
    	for(int i=0; i<size; i++){
    		scan.addColumn(tb.m_family.get(i).getBytes(), tb.m_column.get(i).getBytes()); 
    	}
    	
    	return scan;
	}
	
	private void scanSetLimit(Scan scan, int limit){
		try {
			Method[] m=null;
			if (ImUtils.checkFunctionExisted(scan.getClass(), "setLimit", m)){
				m[0].invoke(scan.getClass(), limit);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 		
	}
	
	// 全表扫描	
	public Object doHbaseQuery( TableInfo tb ) throws IOException
	{      
		//System.out.println("doHbaseQuery");
		return null;
	}	
	
	public Object doHbaseQuery(  Object obj[] ) throws IOException
	{  
		return null;
	}
	
	public Object hbaseQuery(  Object obj[] ) throws IOException
	{      
		try {
	   		if (obj.length != 2) {
				throw new RQException("hbaseScan hbaseQuery tableName or rowKey is not right");
			}			
			HbaseDriverCli client = (HbaseDriverCli)obj[0];
			if (client == null) {
				throw new RQException("hbaseScan hbaseQuery HbaseDriverCli is null");
			}

			if (client.m_conn == null) {
				throw new RQException("hbaseScan hbaseQuery hConn is null");
			}
			  
        } catch (Exception e) {            
            e.printStackTrace();
        }
	 
		 return null;
	}
	/*
	 * hbase_scan(client,表名,列1:类型:别名,列2:类型:别名,...;rowPrefix:x,
	 * filter:f,limit:l,version
	 * :i,startRow:x,stopRow:x,timeRange:[t1,t2],timeStamp:t)
	 */
	protected void doTableParam(Context ctx, ArrayList<Expression> list) {
		int size = list.size();
		// System.out.println("param size2 = " +size);
		if (size % 2 == 1) {
			throw new RQException("scan params not map key structure");
		}

		String key, v;

		for (int i = 0; i < size; i += 2) {
			key = ((Expression) list.get(i)).toString();
			v = ((Expression) list.get(i + 1)).toString();
			//System.out.printf("k=" + key + " v=" + v);
			if (key.compareToIgnoreCase("filter") == 0) {
				Object val = ((Expression) list.get(i + 1)).calculate(ctx);
				Filter f = (Filter) (val);
				m_tableInfo.setFilter(f);
			} else if (key.compareToIgnoreCase("rowPrefix") == 0) {
				//System.out.println("rowPrefix = " + v);
				m_tableInfo.setRowPrefixFilter(v);
			} else if (key.compareToIgnoreCase("limit") == 0) {
				m_tableInfo.setLimit(Integer.parseInt(v));
			} else if (key.compareToIgnoreCase("version") == 0) {
				m_tableInfo.setVersion(Integer.parseInt(v));
			} else if (key.compareToIgnoreCase("startRow") == 0) {
				m_tableInfo.setStartRow(v);
			} else if (key.compareToIgnoreCase("stopRow") == 0) {
				m_tableInfo.setStopRow(v);
			} else if (key.compareToIgnoreCase("timeRange") == 0) {
				String ss = v.replace("[", "");
				ss = ss.replace("]", "");
				ss = ss.replace(" ", "");
				ss = ss.replace("\t", "");
				String[] ls = ss.split(",");
				if (ls[0] != null && ls[0] != "") {
					m_tableInfo.setMinTimeStamp(ImUtils.objectToLong(ls[0]));
				}
				if (ls[1] != null && ls[1] != "") {
					m_tableInfo.setMaxTimeStamp(ImUtils.objectToLong(ls[1]));
				}
			} else if (key.compareToIgnoreCase("timeStamp") == 0) {
				m_tableInfo.setTimeStamp(ImUtils.objectToLong(v));
			}
		}

		// m_tableInfo.printTest("scan param part");
	}

	//处理传递参数
	protected void doTableColumn(Context ctx, ArrayList<Expression> list) {
		int size = list.size();
		Object obj = new Object();
		// System.out.println("table size = " +size);

		String s, ss;
		m_chkType = CheckType.CHECK_TABLE;
		/*1. Get conn, "employee","row2","company:name":string,"company:tel":string:ctel;
		  2. Scan conn, "employee","company:name":string,"company:tel":string:ctel;
		*/
		Matcher m[] = new Matcher[1];
		for (int i = 0; i < size; i++) {
			if (list.get(i)!=null){
				ss = ((Expression) list.get(i)).getIdentifierName();
				s = ss.replace("\"", "");				
				if (ImUtils.isRegExpMatch(s, "(\\w+):(\\w+)", m)){ // family:column
					m_chkType = CheckType.CHECK_COLUMN;
				}

				//System.out.println("CHECK_COLUMN:::" + s + " type=" + ((Expression) list.get(i)).getExpValueType(ctx));
				if (m_chkType == CheckType.CHECK_TABLE) {
					obj = ((Expression) list.get(i)).calculate(ctx);
					if (i == 0) {
						m_tableInfo.setConnect(obj);
					} else if (i == 1) {
						m_tableInfo.setTableName((String) obj);
						if (m_tableInfo.m_oprationType ==OprationType.OPRATION_SCAN){
							m_chkType = CheckType.CHECK_COLUMN;
						}					
					}else if (i == 2) {// for get rowkey
						if (m_tableInfo.m_oprationType == OprationType.OPRATION_SCAN){
							throw new RQException("scan params not rowkey after tableName");
						}
						m_tableInfo.setRowkey((String) obj);
						if (m_tableInfo.m_oprationType ==OprationType.OPRATION_GET){
							m_chkType = CheckType.CHECK_COLUMN;
						}					
					}
				} else if (m_chkType == CheckType.CHECK_COLUMN) {
					parseFamilyColumn(m); // A100 for column
					// System.out.println("CHECK_COLUMN:::" + m_tmpColumnName);
				} else if (m_chkType == CheckType.CHECK_TYPE) {
					//System.out.println("checType = " + s);
					if (ImUtils.checkColumnType(m_tableInfo, m_tmpColumnName, s)) { // A100 for column type
						m_chkType = CheckType.CHECK_ALIAS;
					}
				} else if (m_chkType == CheckType.CHECK_ALIAS) {// for alias
					// A102 alias
					m_tableInfo.setColumnAlias(m_tmpColumnName, s);
					// System.out.println("CNAME: " + m_tmpColumnName + "==>"+ s)
					m_chkType = CheckType.CHECK_COLUMN;
				}
			}else{
				m_tableInfo.setColumnType(m_tmpColumnName, "type");
				m_chkType = CheckType.CHECK_ALIAS;
			}//endif
		} //endfor

		//m_tableInfo.printTest("scan table part");
	}

	/*1. Get conn, "employee","row2","company:name":string,"company:tel":string:ctel;
	  2. Scan conn, "employee","company:name":string,"company:tel":string:ctel;
	*/
	protected void doTableColumn(Context ctx, IParam param) {
		int size = param.getSubSize();
		Object obj = new Object();

		String s = "", ss;
		ArrayList<Expression> list = new ArrayList<Expression>();
		m_chkType = CheckType.CHECK_TABLE;
		
		for (int i = 0; i < size; i++) {
			if (param.getSub(i).isLeaf() ){ //无条件
				if (i==0){ // for conn handle;
					obj = param.getSub(i).getLeafExpression().calculate(ctx);
				}else{
					s = (String)param.getSub(i).getLeafExpression().calculate(ctx);
				}
			}else{ //有条件
				list.clear();
				param.getSub(i).getAllLeafExpression(list);
			}
			
			if (m_chkType == CheckType.CHECK_TABLE) {
				if (i == 0) {
					m_tableInfo.setConnect(obj);
				} else if (i == 1) {
					m_tableInfo.setTableName(s);
					if (m_tableInfo.m_oprationType ==OprationType.OPRATION_SCAN){
						m_chkType = CheckType.CHECK_COLUMN;
					}					
				}else if (i == 2) {// for get rowkey
					m_tableInfo.setRowkey(s);
					if (m_tableInfo.m_oprationType ==OprationType.OPRATION_GET){
						m_chkType = CheckType.CHECK_COLUMN;
					}					
				}
			} else {
				Matcher m[] = new Matcher[1];
				for(int j=0; j<list.size(); j++){
					if (list.get(j)!=null){
						ss = ((Expression) list.get(j)).getIdentifierName();	
						s = ss.replace("\"", "");
						if (ImUtils.isRegExpMatch(s, "(\\w+):(\\w+)", m)){ // family:column
							m_chkType = CheckType.CHECK_COLUMN;
						}
						if (m_chkType == CheckType.CHECK_COLUMN) {				
							parseFamilyColumn(m); // A100 for column
							// System.out.println("CHECK_COLUMN:::" + m_tmpColumnName);
						} else if (m_chkType == CheckType.CHECK_TYPE) {
							if (ImUtils.checkColumnType(m_tableInfo, m_tmpColumnName, s)) { // A100 for column type
								m_chkType = CheckType.CHECK_ALIAS;
							}
						} else if (m_chkType == CheckType.CHECK_ALIAS) {// for alias
							m_tableInfo.setColumnAlias(m_tmpColumnName, s);
							// System.out.println("CNAME: " + m_tmpColumnName + "==>"+ s );
							m_chkType = CheckType.CHECK_COLUMN;
						}
					}else{
						m_chkType = CheckType.CHECK_ALIAS;
						m_tableInfo.setColumnType(m_tmpColumnName,"byte");
					} //endif
				} //endfor
			} //endif
		}
	}
	
	// 解析Family:Column
	// return bRet: 是否为Family:Column
	protected void parseFamilyColumn(Matcher m[]) {
		if (m[0].groupCount() == 2) { // A101 family, column, alias
			m_tmpColumnName = m[0].group(1) + "." + m[0].group(2);
			m_tableInfo.setFamily(m[0].group(1));
			m_tableInfo.setColumn(m[0].group(2));
			m_tableInfo.setColumnAlias(m_tmpColumnName, m_tmpColumnName);
			m_tableInfo.setColumnType(m_tmpColumnName, "byte");
		}else{
			throw new RQException("parseFamilyColumn matcher groupCount is not 2");
		}
		m_chkType = CheckType.CHECK_TYPE;		
	}
		  
	//将ResultScanner转换成Talbe, 
  	@SuppressWarnings("deprecation")
	public static Table toTable(ResultScanner scanner) throws IOException {
  		if ( scanner == null) return null;
  		String[] colNames = null;
  		
  		Table table = null;
  		boolean bFirst = true;
  		Object[] objs=null;
  		List<String> list = new ArrayList<String>();  		
  		List<String> vlist = new ArrayList<String>();
  		
  		String family, column, fullName;
  		for(Result res:scanner){
  			String rowkey=Bytes.toString(res.getRow());
  			
  			if (bFirst){
  				bFirst = false;	
  				getColumnArray(res, list, vlist);
  				colNames = vlist.toArray(new String[vlist.size()]);
  				table = new Table(colNames);
  				//list.clear();
  							
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
  		//有记录则改名
  		if (list.size()>0 && table!=null ){
	  		String[] cols = list.toArray(new String[list.size()]);
	  		table.rename(colNames, cols);
  		}
  		return table;
  	}
  	
  	public static Table toTable(ResultScanner scanner, TableInfo tb, int limit) {
  		if ( scanner == null) {
  			throw new RQException("toTable param scanner is not null" );
  		}
  		
  		if ( tb == null) {
  			throw new RQException("toTable param TableInfo is not null" );
  		}
  		if (tb.m_family.size()==0){
  			try {
				return toTable(scanner);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
  		}
  		
  		Table table = null;
  		Object[] objs=null;
  		String[] colNames = null;
  		String family, column,fullName,sAliasKey = "";
  		
  		int nCount = 0, nCol = 0;
  		int colSize = tb.m_columnAlias.size()+1; 
  		colNames = new String[colSize];
  		colNames[0] = "rowkey";

  		for(String val: tb.m_columnAlias.values()){
  			colNames[++nCol] = val;
  		}
  		table = new Table(colNames);
  		for(Result res:scanner){
  			objs = new Object[colSize]; 
  			objs[0] = res.getRow();
  			if (objs[0] instanceof Integer){
  				objs[0] = Integer.parseInt(String.valueOf(objs[0]));
  			}else{
  				objs[0] = Bytes.toString(res.getRow());
  			}
			nCol = 0;
  			Record r = table.newLast(objs);
  			
  			List<Cell> cells = res.listCells();
  	        for (Cell c : cells) {
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
  			if (limit > 0 && ++nCount>=limit) break; //最大记录数;
  		}
  		
  		return table;
  	}
  	
  	public static void getColumnArray(Result result, List<String> retCols, List<String> retVCol ) {
		String family="", column="";
		String oldFamily="";
		retCols.clear();
		retVCol.clear();
		retCols.add("rowkey");
		retVCol.add("rowkey");
		
		List<Cell> cells = result.listCells();		
        for (Cell c : cells) {
			//System.out.println("val = "+Bytes.toString(kv.getValue()));
			family= Bytes.toString(CellUtil.cloneFamily(c));
			column= Bytes.toString(CellUtil.cloneQualifier(c));
			if (column.compareTo("_0")==0){
				continue;
			}
			if (!family.equals(oldFamily)){
				retCols.add(family+"."+column);
			}else{
				retCols.add("~."+column);
			}
			retVCol.add(family+"."+column);
            oldFamily = family;
		}
	}
	
}
