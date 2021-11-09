package com.raqsoft.lib.informix.helper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.informix.jdbc.IfmxResultSetMetaData;
import com.informix.jdbc.IfxConnection;
import com.raqsoft.common.Logger;
import com.raqsoft.common.SQLParser;

public class ImTableInfo {
	public Map<Integer, ImColumn> m_colMap;
	private int m_colSize = 0;
	public  int m_dataSize = 0;
	public String m_saveFileName;
	public String m_tableName;
	public String[] m_colNames;
	public String	m_encode;
	
	public ImTableInfo() {
		m_colSize = 0;
		m_colMap = new HashMap<>();
	}
	
	public Map<Integer, ImColumn> getColumnInfo(){
		return m_colMap;
	}

	public void addColumn(ImColumn col){
		m_colMap.put(col.nIndex, col);
		m_colSize+=col.nSize;
	}
	
	public int getColSize(){
		return m_colSize;
	}
	
	public void reset(){
		m_colSize = 0;
		m_tableName = "";
		m_colNames = null;
		m_colMap.clear();
	}
	
	public String[] parseInfo(String strTable) {
		String[] colNames = null;
		do {
			if (strTable == null)
				break;
			// String[] ary = strTable.split("[\r|\n]");
			String[] ary = strTable.split(",");
			if (ary.length <= 0)
				break;

			int nIdx = 0;
			colNames = new String[ary.length];
			for (String s : ary) {
				ImColumn col = new ImColumn();
				//System.out.println("val = " + s);
				getSizeByTypeName(s.toLowerCase(), col);
				colNames[nIdx]=col.colName;
				col.nIndex = nIdx++;
				m_colMap.put(col.nIndex, col);
				m_colSize+=col.nSize;
				
				//System.out.println("index = " + col.nIndex+ " colName=" + col.colName +
				//		" type=" + col.colType + " len="+col.nSize);
			}
		} while (false);

		return colNames;
	}

	public void getSizeByTypeName(String sLine, ImColumn column) {
		short nRet = 0;
		do {
			if (sLine == null)
				break;
			else if (sLine == "")
				break;

			String[] subAry;
			subAry = sLine.split(" ");
			String typeName = "";
			if (subAry.length > 1) {
				column.colName = subAry[0];
				typeName = subAry[1];
			} else {
				System.out.println(sLine + "not split");
				break;
			}

			if (typeName.indexOf("integer") >= 0) {
				column.colType = "integer";
				column.nType = ImColumn.DATA_TYPE.TYPE_INTEGER;
				nRet = 4;
			}else if (typeName.indexOf("bigint") >= 0) {
				column.colType = "bigint";
				column.nType = ImColumn.DATA_TYPE.TYPE_BIGINT;
				nRet = 8;
			}else if (typeName.indexOf("bigserial") >= 0) {
				column.colType = "bigserial";
				column.nType = ImColumn.DATA_TYPE.TYPE_BIGSERIAL;
				nRet = 8;
			} else if (typeName.indexOf("decimal") >= 0 ||
					typeName.indexOf("money") >= 0) {
				
				String regExp = "[\\s\\S]+\\((\\d+)#(\\d+)\\)";
				Pattern p = Pattern.compile(regExp);
				Matcher match = p.matcher(typeName);
				// System.out.println(retMatch[0].groupCount());

				if (match.find()) {
					//System.out.println("vSize=" + match.groupCount());
					column.nStartSize = (short) Integer.parseInt(match.group(1));
					column.nEndSize = (short) Integer.parseInt(match.group(2));
					if(column.nEndSize%2==0){
						nRet = (short) ((column.nStartSize + 3)/2);
					}else{
						nRet = (short) ((column.nStartSize + 4)/2);
					}
				}
				if (typeName.indexOf("decimal") >= 0){
					column.colType = "decimal";
					column.nType = ImColumn.DATA_TYPE.TYPE_DECIMAL;
				}else{
					column.colType = "money";
					column.nType = ImColumn.DATA_TYPE.TYPE_MONEY;
				}
				
				column.nType = ImColumn.DATA_TYPE.TYPE_DECIMAL;
			} else if (typeName.indexOf("datetime") >= 0) {
				nRet = 11;
				column.colType = "datetime";
				column.nType = ImColumn.DATA_TYPE.TYPE_DATETIME;
			} else if (typeName.indexOf("date") >= 0) {
				nRet = 4;
				column.colType = "date";
				column.nType = ImColumn.DATA_TYPE.TYPE_DATE;
			} else if( (typeName.indexOf("lvarchar") >= 0 ||
						typeName.indexOf("varchar") >= 0 ||
						typeName.indexOf("nchar") >= 0 ||
					   typeName.indexOf("char") >= 0) ) {
				String regExp = "[\\s\\S]+\\((\\d+)\\)";
				String regExp2 = "[\\s\\S]+\\((\\d+)#(\\d+)\\)";
				Pattern p = Pattern.compile(regExp);
				Matcher match = p.matcher(typeName);
				// System.out.println(retMatch[0].groupCount());

				if (match.find()) {
					//System.out.println("vSize=" + match.groupCount());
					nRet = (short) Integer.parseInt(match.group(1));
				}else{ //varcharer varchar(1#1)
					p = Pattern.compile(regExp2);
					match = p.matcher(typeName);
					if (match.find()) {
						//System.out.println("vSize=" + match.groupCount());
						nRet = (short) Integer.parseInt(match.group(1));
					}
				}
				if (typeName.indexOf("lvarchar") >= 0){
					column.colType = "lvarchar";
					column.nType = ImColumn.DATA_TYPE.TYPE_LVARCHAR;
					nRet += 3; //标识长度
				}else if (typeName.indexOf("varchar") >= 0){
					column.colType = "varchar";
					column.nType = ImColumn.DATA_TYPE.TYPE_VARCHAR;
					nRet += 1; //标识长度
				}else if (typeName.indexOf("nchar") >= 0){
					column.colType = "nchar";
					column.nType = ImColumn.DATA_TYPE.TYPE_NCHAR;
				}else{
					column.colType = "char";
					column.nType = ImColumn.DATA_TYPE.TYPE_CHAR;
				}		
			} else if (typeName.indexOf("smallfloat") >= 0) {
				column.colType = "smallfloat";
				column.nType = ImColumn.DATA_TYPE.TYPE_SMALLFLOAT;
				nRet = 4;
			} else if (typeName.indexOf("float") >= 0) {
				column.colType = "float";
				column.nType = ImColumn.DATA_TYPE.TYPE_FLOAT;
				nRet = 8;				
			} else if (typeName.indexOf("interval") >= 0) {
				column.colType = "interval";
				column.nType = ImColumn.DATA_TYPE.TYPE_INTERVAL;
				nRet = 4;				
			} else if (typeName.indexOf("serial") >= 0) {
				column.colType = "serial";
				column.nType = ImColumn.DATA_TYPE.TYPE_SERIAL;
				nRet = 4;
			} else if (typeName.indexOf("smallint") >= 0) {
				column.colType = "smallint";
				column.nType = ImColumn.DATA_TYPE.TYPE_SMALLINT;
				nRet = 2;
			} else if (typeName.indexOf("boolean") >= 0) {
				column.colType = "boolean";
				column.nType = ImColumn.DATA_TYPE.TYPE_BOOLEAN;
				nRet = 2;
			} else if (typeName.indexOf("int8") >= 0) {
				column.colType = "int8";
				column.nType = ImColumn.DATA_TYPE.TYPE_INT8;
				nRet = 10; //理论应该是8字节，但实际占10个字节大小
			} else  {
				System.out.println("typeName = " + typeName + " not knowned");
			}
		} while (false);

		column.nSize = nRet;
	}

	public void getSizeByDataType(String typeName, ImColumn column) {
		short nRet = 0;
		do {
			if (typeName == null)
				break;
			else if (typeName == "")
				break;

			if (typeName.indexOf("bigint") >= 0) {
				column.colType = "bigint";
				column.nType = ImColumn.DATA_TYPE.TYPE_BIGINT;
				nRet = 8;
			}else if (typeName.indexOf("bigserial") >= 0) {
				column.colType = "bigserial";
				column.nType = ImColumn.DATA_TYPE.TYPE_BIGSERIAL;
				nRet = 8;
			} else if (typeName.indexOf("decimal") >= 0 ||
					   typeName.indexOf("money") >= 0) {				
				
				if(column.nEndSize%2==0){
					nRet = (short) ((column.nStartSize + 3)/2);
				}else{
					nRet = (short) ((column.nStartSize + 4)/2);
				}

				if (typeName.indexOf("decimal") >= 0){
					column.colType = "decimal";
					column.nType = ImColumn.DATA_TYPE.TYPE_DECIMAL;
				}else{
					column.colType = "money";
					column.nType = ImColumn.DATA_TYPE.TYPE_MONEY;
				}
				
				column.nType = ImColumn.DATA_TYPE.TYPE_DECIMAL;
			} else if (typeName.indexOf("datetime") >= 0 ||
					   typeName.indexOf("interval") >= 0) {
				int len = (column.nColLength - column.nColLength % 256) / 256;
				nRet =(short) ((short) (len+(len%2==0?3:4))/2);
				if (typeName.indexOf("datetime") >= 0){
					column.colType = "datetime";
					column.nType = ImColumn.DATA_TYPE.TYPE_DATETIME;
				}else{
					column.colType = "interval";
					column.nType = ImColumn.DATA_TYPE.TYPE_INTERVAL;
				}
			} else if (typeName.indexOf("date") >= 0) {
				nRet = 4;
				column.colType = "date";
				column.nType = ImColumn.DATA_TYPE.TYPE_DATE;
			} else if( (typeName.indexOf("lvarchar") >= 0 ||
						typeName.indexOf("varchar") >= 0 ||
						typeName.indexOf("nchar") >= 0 ||
					   typeName.indexOf("char") >= 0) ) {
				nRet = column.nSize;
				if (typeName.indexOf("lvarchar") >= 0){
					column.colType = "lvarchar";
					column.nType = ImColumn.DATA_TYPE.TYPE_LVARCHAR;
					nRet += 3; //标识长度
				}else if (typeName.indexOf("varchar") >= 0){
					column.colType = "varchar";
					column.nType = ImColumn.DATA_TYPE.TYPE_VARCHAR;
					nRet += 1; //标识长度
				}else if (typeName.indexOf("nchar") >= 0){
					column.colType = "nchar";
					column.nType = ImColumn.DATA_TYPE.TYPE_NCHAR;
				}else{
					column.colType = "char";
					column.nType = ImColumn.DATA_TYPE.TYPE_CHAR;
				}		
			} else if (typeName.indexOf("smallfloat") >= 0) {
				column.colType = "smallfloat";
				column.nType = ImColumn.DATA_TYPE.TYPE_SMALLFLOAT;
				nRet = 4;
			} else if (typeName.indexOf("float") >= 0) {
				column.colType = "float";
				column.nType = ImColumn.DATA_TYPE.TYPE_FLOAT;
				nRet = 8;				
			} else if (typeName.indexOf("serial") >= 0) {
				column.colType = "serial";
				column.nType = ImColumn.DATA_TYPE.TYPE_SERIAL;
				nRet = 4;
			} else if (typeName.indexOf("smallint") >= 0) {
				column.colType = "smallint";
				column.nType = ImColumn.DATA_TYPE.TYPE_SMALLINT;
				nRet = 2;
			} else if (typeName.indexOf("boolean") >= 0) {
				column.colType = "boolean";
				column.nType = ImColumn.DATA_TYPE.TYPE_BOOLEAN;
				nRet = 2;
			} else if (typeName.indexOf("int8") >= 0) {
				column.colType = "int8";
				column.nType = ImColumn.DATA_TYPE.TYPE_INT8;
				nRet = 10; //理论应该是8字节，但实际占10个字节大小
			}else if (typeName.indexOf("integer") >= 0 ||
					  typeName.indexOf("int") >= 0) {
				column.nType = ImColumn.DATA_TYPE.TYPE_INTEGER;
				nRet = 4;
			} else  {
				System.out.println("typeName = " + typeName + " not knowned");
			}
		} while (false);

		column.nSize = nRet;
	}
	
	public void initTableInfo(Connection conn, String sql)
    {
		String[] cols = null;
    	try {
    		reset();
    		String sSql = sql.replace("{+full(lineitem)},", "");
    		SQLParser parser = new SQLParser(sSql);
    		String tableName = parser.getClause(SQLParser.KEY_FROM);
    		String colNames = parser.getClause(SQLParser.KEY_SELECT);
    		String sWhere = parser.getClause(SQLParser.KEY_WHERE);
//    		String sGroupby = parser.getClause(SQLParser.KEY_GROUPBY);
//    		String sHaving = parser.getClause(SQLParser.KEY_HAVING);
//    		String sOrderby = parser.getClause(SQLParser.KEY_ORDERBY);
    		if (tableName ==null) return;
    		IfxConnection sqlConn= (IfxConnection)conn;
    		m_encode = sqlConn.getdbEncoding();
    		//String local = sqlConn.getdbLocale();
    		
    		Statement stmt = conn.createStatement();
    		//System.out.println("testSql="+sql);
    		//ResultSet rs2 = stmt.executeQuery(sql); //for pamire test.
    		String regEx = "first\\s+(\\d+)";
    		Pattern p = Pattern.compile(regEx);
    		Matcher m = p.matcher(colNames);
    		
    		String newSql = "";
    		if (m.find()){
    			colNames = colNames.replaceFirst(m.group(1), "1");
    			newSql = "select "+colNames+" from " + tableName;
    		}else{
    			newSql = "select first 1 " +colNames+" from " + tableName;
    		}
    		if (sWhere!=null){
    			newSql += " where " + sWhere;
    		}
//    		if (sGroupby!=null){
//    			newSql += " group by " + sGroupby;
//    		}
//    		if (sHaving!=null){
//    			newSql += " having " + sHaving;
//    		}
//    		if (sOrderby!=null){
//    			newSql += " order by " + sOrderby;
//    		}
    		System.out.println("testSql="+newSql);
			ResultSet rs = stmt.executeQuery(newSql);
			IfmxResultSetMetaData meta = (IfmxResultSetMetaData)rs.getMetaData();
			cols = new String[meta.getColumnCount()];
			m_tableName = meta.getTableName(1);
			for(int i=1; i<=meta.getColumnCount(); i++){	
				ImColumn column = new ImColumn();
				column.colName = meta.getColumnName(i);
				column.nIndex = i-1;
				column.colType = meta.getColumnTypeName(i);
				column.nColLength = (short)meta.getEncodedLength(i);
				column.nSize = (short)meta.getColumnDisplaySize(i);
				column.nStartSize =(short) meta.getPrecision(i);
				column.nEndSize = (short)meta.getScale(i);
				getSizeByDataType(column.colType,column);
				addColumn(column);
//				System.out.println("index="+column.nIndex 
//						+ " name="+column.colName 
//						+ " type=" + column.nType
//						+ " typeName=" + column.colType
//						+ " nSize=" + column.nSize
//						+ " nStartSize=" + column.nStartSize
//						+ " nEndSize=" + column.nEndSize
//						);
				
				cols[i-1] = column.colName.toLowerCase();
			}
			stmt.close();
			m_colNames = cols;

			m_dataSize = Integer.MAX_VALUE;
    	} catch (SQLException e) {
    		Logger.error(e.getStackTrace());
		}
    }
	
	
	
	public short getDataLeng(String colName) {
		short nLen = 0;
		Set<Integer> set = m_colMap.keySet();
		ImColumn col = null;
		for (Iterator<Integer> iter = set.iterator(); iter.hasNext();) {
			Integer key = (Integer) iter.next();
			col = (ImColumn) m_colMap.get(key);
			if (col.colName == colName) {
				nLen = col.nSize;
				break;
			}
		}

		return nLen;
	}

	public short getDataLeng(int idx) {
		ImColumn col = (ImColumn) m_colMap.get(idx);
		if(col!=null){
			return col.nSize;
		}else{
			return 0;
		}
	}
	
	public ImColumn getCellColumn(int idx) {
		ImColumn col = (ImColumn) m_colMap.get(idx);		
		return col;
	}
	
	public static float byte2float(byte[] b, int index) {    
	    int l;                                             
	    l = b[index + 0];                                  
	    l &= 0xff;                                         
	    l |= ((long) b[index + 1] << 8);                   
	    l &= 0xffff;                                       
	    l |= ((long) b[index + 2] << 16);                  
	    l &= 0xffffff;                                     
	    l |= ((long) b[index + 3] << 24);                  
	    return Float.intBitsToFloat(l);                    
	}  
	
	public static byte[] double2Bytes(double d) {  
        long value = Double.doubleToRawLongBits(d);  
        byte[] byteRet = new byte[8];  
        for (int i = 0; i < 8; i++) {  
            byteRet[i] = (byte) ((value >> 8 * i) & 0xff);  
        }  
        return byteRet;  
    }  
	
	public static double bytes2Double(byte[] arr,int offset) {  
        long value = 0;  
        for (int i = 0; i < 8; i++) {  
            value |= ((long) (arr[offset+i] & 0xff)) << (8 * i);  
        }  
        return Double.longBitsToDouble(value);  
    }  
	
	public static short bytes2short(byte[] arr,int offset) {  
		return (short)(((arr[offset+1] & 0x00FF) << 8) | (arr[offset] & 0x00FF));
    }  
	
	//buflen
	public static void reverseOrder(byte[] buf,int offset, int bufLen) 
	{
		byte bt;
		for(int i=0; i<bufLen/2; i++){
			bt = buf[offset+i];
			buf[offset+i] = buf[offset+bufLen-1-i];
			buf[offset+bufLen-1-i] = bt;
		}
	}
}
