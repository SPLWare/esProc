package com.raqsoft.lib.informix.helper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raqsoft.common.Logger;
import com.raqsoft.common.SQLParser;

public class ImSQLParser {
	private Fragment	m_frag;
	private String 		m_sql;
	private boolean m_bOk = true;
	private String m_tableName;
	private String m_colNames;
	private String m_sWhere;
	private String m_sGroupby;
	private String m_sHaving;	
	private String m_sOrderby;	
	
	public ImSQLParser(Fragment frag, String sql){
		if (frag==null){
			m_sql = sql;
		}else{
			m_frag = frag;
			SQLParser parser = new SQLParser(sql);
    		m_tableName = parser.getClause(SQLParser.KEY_FROM);
    		m_colNames = parser.getClause(SQLParser.KEY_SELECT);
    		m_sWhere = parser.getClause(SQLParser.KEY_WHERE);
    		m_sGroupby = parser.getClause(SQLParser.KEY_GROUPBY);
    		m_sHaving = parser.getClause(SQLParser.KEY_HAVING);
    		m_sOrderby = parser.getClause(SQLParser.KEY_ORDERBY);
			m_bOk = parserSql(sql);
		}
	}
	
	public String getSql(){
		return m_sql;
	}
	
	private boolean parserSql(String sqlVal){
		boolean bRet = true;
		do{
			String segWhere="";
			/********************************
			 * 区间参数传递时检验数据的有效性
			 * 0. 区间: [min,max],start不可缺
			   1. start=s,end=m 从s到m
			 * 2. start最小为1, end为最后一个则不设置
			 * 3. start=end,或end=0,则只有start
			 */
			int nStart = m_frag.getSegmentStart();
			int nEnd = m_frag.getSegmentEnd();
			if (nStart ==0 && nEnd ==0){
				break;
			}
			
    		//生成新的sqlVal
    		sqlVal = "select "+m_colNames+" from " + m_tableName;
			if (m_sWhere!=null){
				sqlVal += " where (" + m_sWhere+")";
    		}
			String sMax = "";
			boolean bLessSkip = false;
			if (m_frag.getRuleType()=='L' ){
				// [null, m]
				// [s, null]
				Object o;
				if (nEnd == 0) nEnd=nStart;
				segWhere = m_frag.getFieldName() + " in (";
				List<Object> ls = m_frag.getPartitionMap();				
				for(int i=nStart-1;i<nEnd; i++){
					o = ls.get(i);
					if (o==null){
						segWhere += ",null";
					}else{
						segWhere += ", "+o;
					}
				}
				segWhere += ")";				
				segWhere = segWhere.replace(",null)", ") or " + m_frag.getFieldName() + " is null"); //last
				if (segWhere.indexOf("in (,null")>1){ //first
					segWhere = segWhere.replace(" in (,null", " in (");
					segWhere +=" or " + m_frag.getFieldName() + " is null"; 
				}
				segWhere = segWhere.replace("in (,", "in (");
				if (segWhere.indexOf("in ()")>0){
					segWhere = segWhere.replace( m_frag.getFieldName() +" in ()", "");
				}
				
				if (sqlVal.indexOf("where")==-1){
					if (segWhere.indexOf(" or")==0){
						segWhere = segWhere.replace( " or", "");
					}else if (segWhere.indexOf(" and")==0){
						segWhere = segWhere.replace( " and", "");
					}
					sqlVal += " where (" + segWhere+")";
				}else if (segWhere.indexOf(" or")==0){
					sqlVal += " " + segWhere;
				}else{
					sqlVal += " and (" + segWhere+")";
				}
				
				break;
			}else{
				if (nEnd == m_frag.getPartitionCount() ||
					nStart == m_frag.getPartitionCount()){ //last
					nEnd = -1;
					if (m_frag.getMaxValue()!=null){
						sMax = m_frag.getMaxValue().toString();
						//System.out.println("parse max = " + sMax);
						nEnd = -2;
					}
				}else if(nStart == nEnd){ // nStart=End,or end=0;
					nEnd = 0;
				}
				
				if(nStart==1 && m_frag.getPartition(0)==null){
					bLessSkip = true;
					if (nEnd==-1){ //[null,]
						break;
					}
				}				
			}
			
			String sComp = "";
			String aComp[] = null;
			String lessSql = "";
			boolean bComparison = false;
			int nType = m_frag.getFieldType();
			switch(nType){
			case java.sql.Types.TIMESTAMP:
			{		
				sComp = m_frag.getComparison(nStart-1);
				if (sComp==null){
					aComp = new String[]{">=","<"};
				}else{
					aComp = sComp.split(";");
					bComparison = true;
				}
				if (m_frag.getRuleType()=='N'){
					lessSql = " AND ("+m_frag.getFieldName()+ aComp[0]+ " date("+m_frag.getPartition(nStart-1)+ ") )";
					if (bLessSkip) lessSql="";
					if (nEnd == -2){
						if (bComparison){
							sComp = m_frag.getComparison(m_frag.getComparisonCount()-1);
							aComp = sComp.split(";");
						}
						segWhere = "(("+m_frag.getFieldName()+aComp[1]+ "  date("+sMax+ "))"+ lessSql + " )";
					}else if (nEnd==-1){
						segWhere = " ("+m_frag.getFieldName()+aComp[0]+ " date("+m_frag.getPartition(nStart-1)+")  )";
					}else if (nEnd==0){
						segWhere = "(("+m_frag.getFieldName()+aComp[1]+ "  date("+m_frag.getPartition(nStart)+ ") )" + lessSql + " )";
					}else{
						if (bComparison){
							sComp = m_frag.getComparison(nEnd-1);
							aComp = sComp.split(";");
						}
						segWhere = "(("+m_frag.getFieldName()+aComp[1]+ "  date("+m_frag.getPartition(nEnd)+ ") )" + lessSql + " )";
					}
				}else{
					sComp = m_frag.getComparison(nStart-1);
					if (sComp==null){
						aComp = new String[]{">=","<"};
					}else{
						aComp = sComp.split(";");
						bComparison = true;
					}
					String sTyle = "%Y-%m-%d %H:%M:%S";
					lessSql = " AND ("+m_frag.getFieldName()+aComp[0]+ " to_date('"+m_frag.getPartition(nStart-1)+ "', '"+sTyle+"') )";
					if (bLessSkip) lessSql="";
					if (nEnd == -2){
						if (bComparison){
							sComp = m_frag.getComparison(m_frag.getComparisonCount()-1);
							aComp = sComp.split(";");
						}
						segWhere = "(("+m_frag.getFieldName()+aComp[1]+ " to_date('"+sMax+ "', '%Y-%m-%d %H:%M:%S') ) "+lessSql+ " )";
					}else if (nEnd==-1){
						segWhere = " ("+m_frag.getFieldName()+aComp[0]+ " to_date('"+m_frag.getPartition(nStart-1)+"', '"+sTyle+"')  )";
					}else if (nEnd==0){
						segWhere = "(("+m_frag.getFieldName()+aComp[1]+ " to_date('"+m_frag.getPartition(nStart)+ "', '"+sTyle+"') )"+lessSql+ " )";
					}else{
						if (bComparison){
							sComp = m_frag.getComparison(nEnd-1);
							aComp = sComp.split(";");
						}
						segWhere = "(("+m_frag.getFieldName()+aComp[1]+ " to_date('"+m_frag.getPartition(nEnd)+ "', '"+sTyle+"') )"+lessSql+ " )";
					}
				}
				break;
			}
			case java.sql.Types.DATE:
			{	
				sComp = m_frag.getComparison(nStart-1);
				if (sComp==null){
					aComp = new String[]{">=","<"};
				}else{
					aComp = sComp.split(";");
					bComparison = true;
				}		
				lessSql = " AND ("+m_frag.getFieldName()+aComp[0]+ " to_date('"+m_frag.getPartition(nStart-1)+"', '%Y-%m-%d') )";
				if (bLessSkip) lessSql="";
				if (nEnd == -2){
					if (bComparison){
						sComp = m_frag.getComparison(m_frag.getComparisonCount()-1);
						aComp = sComp.split(";");
					}
					segWhere = "(("+m_frag.getFieldName()+aComp[1]+ " to_date('"+sMax+  "', '%Y-%m-%d') ) "+ lessSql + ")";
				}else if (nEnd==-1){
					segWhere = " ("+m_frag.getFieldName()+aComp[0]+ " to_date('"+m_frag.getPartition(nStart-1)+"', '%Y-%m-%d')  )";
				}else if (nEnd==0){
					segWhere = "(("+m_frag.getFieldName()+aComp[1]+ " to_date('"+m_frag.getPartition(nStart)+  "', '%Y-%m-%d') ) "+ lessSql + ")";
				}else{
					if (bComparison){
						sComp = m_frag.getComparison(nEnd-1);
						aComp = sComp.split(";");
					}
					segWhere = "(("+m_frag.getFieldName()+aComp[1]+ " to_date('"+m_frag.getPartition(nEnd)+  "', '%Y-%m-%d') ) "+ lessSql + ")";
				}
				break;
			}
			case java.sql.Types.INTEGER:
			case java.sql.Types.TINYINT:
			case java.sql.Types.SMALLINT:
			case java.sql.Types.BIGINT:
			case java.sql.Types.FLOAT:
			case java.sql.Types.REAL:
			case java.sql.Types.DOUBLE:
			case java.sql.Types.NUMERIC:
			case java.sql.Types.DECIMAL:
			{
				sComp = m_frag.getComparison(nStart-1);
				if (sComp==null){
					aComp = new String[]{">=","<"};
				}else{
					aComp = sComp.split(";");
					bComparison = true;
				}
				lessSql = " and (" +m_frag.getFieldName()+aComp[0]+m_frag.getPartition(nStart-1)+")";
				if (bLessSkip){
					lessSql = "";
				}
				if (nEnd == -2){
					if (bComparison){
						sComp = m_frag.getComparison(m_frag.getComparisonCount()-1);
						aComp = sComp.split(";");
					}
					segWhere = "(("+m_frag.getFieldName()+aComp[1]+sMax +")" + lessSql + ")";
				}else if (nEnd==-1){ //[s,]
					segWhere = "(" +m_frag.getFieldName()+aComp[0]+m_frag.getPartition(nStart-1)+")";
				}else if (nEnd==0){//[s]
					segWhere = "(("+m_frag.getFieldName()+aComp[1]+m_frag.getPartition(nStart) +") "+lessSql + ")";
				}else{ //[s,m]
					if (bComparison){
						sComp = m_frag.getComparison(nEnd-1);
						aComp = sComp.split(";");
					}
					segWhere = "(("+m_frag.getFieldName()+aComp[1]+m_frag.getPartition(nEnd) +") "+ lessSql + ")";
				}

				break;
			}
			
			}//endSwitch
			//sql += segWhere;
			if (m_sWhere!=null){
				sqlVal += " and "+segWhere;
    		}else{
    			sqlVal += " where "+segWhere;
    		}    		
    		bRet = true;
		}while(false);
		if (m_sGroupby!=null){
			String col = m_frag.getFieldName().toLowerCase();
			if (m_sGroupby.toLowerCase().indexOf(col)==-1){
				//System.out.println("group by have no column:" + m_frag.getFieldName());
				return false;
			}
			sqlVal += " group by " + m_sGroupby;
			
		}
		if (m_sHaving!=null){
			sqlVal += " having " + m_sHaving;
		}
		
		if (m_sOrderby!=null){
			if (m_frag.getOrderby() == Fragment.ORDER_TYPE.ORDER_FORCE){
				String ord = m_sOrderby.toLowerCase();
				String col = m_frag.getFieldName().toLowerCase();
				String cols = col;
				if (ord.indexOf(col)!=-1){ //存在，但可能不是第一个.
					String a[] = ord.split(",");
					for(String s : a){
						if (s.equals(col)) continue;
						cols +="," + s;
					}
				}else{
					cols += ","+m_sOrderby;
				}
				sqlVal += " order by " + cols;
			}else{
				sqlVal += " order by " + m_sOrderby;
			}
		}else if (m_frag.getOrderby() == Fragment.ORDER_TYPE.ORDER_FORCE){
			sqlVal += " order by " + m_frag.getFieldName();
		}
		m_sql = sqlVal;
		//System.out.println("parse sql = " + sql);
		
		return bRet;
	}
	
	public static boolean isNumeric(String str){ 
		   Pattern pattern = Pattern.compile("[0-9]*"); 
		   Matcher isNum = pattern.matcher(str);
		   return isNum.matches(); 
		}
	
	public static Map<String, Fragment> parseFragInfo(Connection conn, String tables) {
		 //Connection conn = m_connect.conn;
		 String sql = null;
		 if (tables==null){	
			 sql = "select tabname,evalpos,exprtext,strategy from systables a,sysfragments b where a.tabid=b.tabid";
		 }else{
			 sql = "select tabname,evalpos,exprtext,strategy from systables a,sysfragments b " +
				 	  "where a.tabid=b.tabid and a.tabname in("+tables+") ";
		 }
		// System.out.println("sql = " + sql);
		Map<String, Fragment> mapFrag = null;
		try {			 
			 if(conn==null){
				 return null;
			 }
			 
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			mapFrag = new HashMap<String, Fragment>();

			// int i= 0;
			String v = "";
			String tableName = "";
			
			Fragment frag = null;
			// ((d < datetime(2012-01-01 00:00:00.00000) year to fraction(5) ) AND (d >= datetime(2011-01-01 00:00:00.00000) year to fraction(5) ) )
			String regTitle= "\\([\\s]?+\\((.*) ([>=<]+)(.*)\\) and \\((.*) ([>=<]+)(.*)\\)[\\s]?\\)"; 
			String regBracket= "[date|datetime][\\s]?\\([']?([\\d\\s-:]+)[\\d\\s\\.']?+";
			// ((c < DATE ('2011-01-01' ) ) AND (c >= DATE ('2010-01-01' ) ) )
			Pattern pTitle = Pattern.compile(regTitle);
			Pattern pBracket = Pattern.compile(regBracket);
			
			String regExp2= "values\\s?+\\((.*)\\)";
			Pattern pList= Pattern.compile(regExp2);
			
			char ruleType = 'U'; //0:
			while (rs.next()) {
				ruleType = rs.getString(4).toUpperCase().charAt(0);
				if (ruleType == 'R' || ruleType == 'T')
					continue;
				if (tableName.compareToIgnoreCase(rs.getString(1)) != 0) {					
					frag = getMetadata(conn, rs);
					frag.setRuleType(ruleType);
					tableName = rs.getString(1);
					mapFrag.put(tableName, frag);
					//System.out.println("ruleType " +ruleType + " table="+tableName);
					if (rs.getInt(2) == -3) {
						continue;
					}
				}

				if (rs.getInt(2) == -1){
				}else if (ruleType == 'N'){
					v = rs.getString(3).trim().toLowerCase();
					if (rs.getInt(2) == -2){ //interval						
						// interval(        10) day(9) to day
						String regInterval = "interval\\(\\s+(\\d+)[\\s]?+\\)\\s+(.*)\\((.*)to\\s+(.*)";
						Pattern pInterval = Pattern.compile(regInterval);
						Matcher match5 = pInterval.matcher(v);
						if (match5.find()){						
							// month split by N day
							if ("day".equals(match5.group(2)) && "day".equals(match5.group(4))){
								int days = Integer.parseInt(match5.group(1));
								for(int i=0; i<30; i+=days){
									frag.addPartition(i);
								}
							}else{
								frag.addPartition(v);
							}
						}else if(isNumeric(v)){
							frag.setInterval(Integer.parseInt(v));
						}
					}else{
						String regV1 = "values[\\s]?+([<=>]+)[\\s]?+(\\d+)$";
						//VALUES >= 400. AND VALUES < 500.
						String regV2 = "values[\\s?]+([><=]+)[\\s?]+(.*)\\.(.*)values[\\s?]+([><=]+)+(.*)\\.";
						//VALUES < datetime(2013-01-01 00:00:00.00000) year to fraction(5)
						//String regDt= "<[=\\s]+datetime\\(([\\d-]+)\\s(.*)";	
						
						Pattern pV1 = Pattern.compile(regV1);
						Pattern pV2 = Pattern.compile(regV2);
						//Pattern pDt= Pattern.compile(regDt);
						Matcher mV1 = pV1.matcher(v);
						Matcher mV2 = pV2.matcher(v);						
						//Matcher match4 = pDt.matcher(v);
						String sComparision = "";
						if (mV2.find()){
							frag.addPartition(mV2.group(2));
							frag.setMaxValue(mV2.group(5));
							
							if (mV2.group(1).indexOf("<")>=0){
								sComparision = mV2.group(4)+";"+mV2.group(1);
							}else{
								sComparision = mV2.group(1)+";"+mV2.group(4);
							}
							frag.addComparison(sComparision);
						}else if (mV1.find()){ //单边
							if (mV1.group(1).indexOf("<")>=0){
								frag.addPartition(frag.getMaxValue());
								sComparision = ">=;"+mV1.group(1);
							}else{
								frag.addPartition(mV1.group(2));
								sComparision = mV1.group(1)+";<";
							}
							frag.setMaxValue(mV1.group(2));	
							frag.addComparison(sComparision);
						}
//						else if (match4.find()){
//							frag.addPartition(match4.group(1));
//							frag.setMaxValue(match4.group(1));
//						}						
					}
				}else if (ruleType == 'E'){
					v = rs.getString(3).trim().toLowerCase();	
					//System.out.println("v="+v);
					//1,3="col", 2,4=">=<",3,6="val"
					//(a < 10 )
					String regVal2 = "\\((.*) ([>=<]+)(.*)\\)";
					Pattern pVal2 = Pattern.compile(regVal2);
					
					Matcher match = pTitle.matcher(v);
					Matcher mVal2 = pVal2.matcher(v);	
					String sComparision = "";
					if (match.find()) { //1. 双边值
						String sMax = match.group(3).trim();
						String sMin = match.group(6).trim();						
						if (match.group(2).indexOf(">")>=0){
							sMax = match.group(6).trim();
							sMin = match.group(3).trim();
							sComparision = match.group(2)+";"+match.group(5);
						}else{
							sComparision = match.group(5)+";"+match.group(2);
						}
						frag.addComparison(sComparision);
						//System.out.println("min="+sMin+" max="+sMax);
						Matcher match3 = pBracket.matcher(sMin); 
						if (match3.find()) {
							frag.addPartition(match3.group(1)); //minVal							
						}else{
							frag.addPartition(sMin); //minVal
						}
						match3 = pBracket.matcher(sMax);
						if (match3.find()) {
							frag.setMaxValue(match3.group(1)); //maxVal							
						}else{
							frag.setMaxValue(sMax); //maxVal
						}	
					}else if (mVal2.find()) { //2. 单边值
						String sVal = mVal2.group(3).trim();						
						if (mVal2.group(2).indexOf("<")>=0){
							frag.addPartition(frag.getMaxValue());
							frag.setMaxValue(sVal);
							sComparision = ">=;"+mVal2.group(2);
						}else{
							frag.addPartition(sVal);
							sComparision = mVal2.group(2)+ ";<";
						}
						frag.addComparison(sComparision);
					}else{
						if ("remainder".compareToIgnoreCase(v)==0){
							frag.addPartition(frag.getMaxValue());
							frag.setMaxValue(null);
							// >[=]符号
							int lastIdx = frag.getComparisonCount();
							if (lastIdx>0){
								String last = frag.getComparison(lastIdx-1);
								String ary[] = last.split(";");
								last = ary[0]+";<";
								frag.addComparison(last);
							}
						}else{
							frag.addPartition(v);
						}
					}	
				}else if (ruleType == 'L'){
					v = rs.getString(3).trim().toLowerCase();
					Matcher match = pList.matcher(v);
					if (match.find()){
						Object o = match.group(1).trim();
						if (o.toString().compareTo("null")==0){
							frag.addPartition(null);
						} else {
							frag.addPartition(o);
							frag.setMaxValue(o);
						}
					}else{
						frag.addPartition(v);
					}						
				}
				//System.out.println(rs.getString(1) + ": " + rs.getInt(2) + ": " + rs.getString(3));
			}
			stmt.close();
		} catch (SQLException e) {
			Logger.error(e.getStackTrace());
		}

		doFirstFragNull(conn, mapFrag);
		return mapFrag;
	}
	
	private static void doFirstFragNull(Connection conn, Map<String, Fragment> map){
		try {
			Set<Entry<String, Fragment>> set = map.entrySet();
			Iterator<Entry<String, Fragment>> iterator = set.iterator();
			Fragment frag;
			ResultSet rs = null;
			Statement stmt = conn.createStatement();			
			
			String sql = "";
			String col = "";
			while (iterator.hasNext())
			{
				Map.Entry<String, Fragment> mapentry = iterator.next();
				frag = (Fragment)mapentry.getValue();
				col = frag.getFieldName();

				if (frag.getPartitionCount()<1) continue;
				if (frag.getPartition(0)==null && frag.getRuleType()!='L'){
					sql = "select min("+col+") from " + frag.getTableName() + " where "
							+ col +"<"+frag.getPartition(1); 
					rs = stmt.executeQuery(sql);
					if (rs.next()){
						frag.setPartitionVal(0, rs.getObject(1));
					}					
				}
			}
			stmt.close();
		} catch (SQLException e) {
			Logger.error(e.getStackTrace());
		}
	}
	
	private static Fragment getMetadata(Connection conn,ResultSet rs){
		Fragment frag = null;
		try {
			String colName = "";
			frag = new Fragment();
			String tableName = rs.getString(1);
			frag.setTableName(tableName);
			
			if (rs.getInt(2) == -3) { // 字段名称
				colName = rs.getString(3);
			} else {
				// ((d < datetime(2012-01-01 00:00:00.00000) year to fraction(5) ) AND (d >= datetime(2011-01-01 00:00:00.00000) year to fraction(5) ) )
				String regTitle= "\\([\\s]?+\\((.*) [>=<]+(.*)\\) and \\((.*) [>=<]+(.*)\\)[\\s]?\\)"; 
				String regVal2 = "\\((.*) ([>=<]+)(.*)\\)"; //单边值
				Pattern pTitle = Pattern.compile(regTitle);				
				Pattern pVal2 = Pattern.compile(regVal2);
				
				//System.out.println(rs.getString(1) + ": " + rs.getInt(2) + ": " + rs.getString(3));
				String v = rs.getString(3).trim().toLowerCase();													
				Matcher match = pTitle.matcher(v);
				Matcher mVal2 = pVal2.matcher(v);
				if (match.find()) {
					colName = match.group(3);	
				}else if (mVal2.find()) {
					colName = mVal2.group(1);
				}else {						
					Logger.warn("regexp is not match");
					return frag;
				}
			}
	
			String sql = "select first 1 * from " + tableName;		
			Statement subStmt = conn.createStatement();
			ResultSet subRs = subStmt.executeQuery(sql);
			ResultSetMetaData meta = subRs.getMetaData();
			frag.setFieldName(colName);
			for (int i = 1; i <= meta.getColumnCount(); i++) {
				String sColName = meta.getColumnName(i);
				if (colName.compareTo(sColName) == 0) {
					frag.setFieldType(meta.getColumnType(i));
					frag.setFieldTypeStr(meta.getColumnTypeName(i));
					break;
				}
			}	
			subStmt.close();
		} catch (SQLException e) {
			Logger.error(e.getStackTrace());
		}
		
		return frag;
	}
	
	public boolean isOk(){
		return m_bOk;
	}
	
	public String getGroupString(){
		return m_sGroupby;
	}
}
