package com.scudata.lib.olap4j.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.olap4j.Cell;
import org.olap4j.CellSet;
import org.olap4j.Position;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Member;

import com.scudata.common.RQException;
import com.scudata.dm.DataStruct;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Table;

public class ImUtils {
	public static String[] objectArray2StringArray(Object[] objs) {
		return Arrays.asList(objs).toArray(new String[0]);
	}

	/**
	 * If enabled and applicable to this command, print the field headers for
	 * the output.
	 * 
	 * @param qp
	 *            Driver that executed the command
	 * @param out
	 *            PrintStream which to send output to
	 */

	// 是否合法的sql语句.
	public static boolean isLegalSql(String strSql) {
		String span = strSql.toUpperCase();// 测试用sql语句
		System.out.println(span);
		String column = "(\\w+\\s*(\\w+\\s*){0,1})";// 一列的正则表达式 匹配如 product p
		String columns = column + "(,\\s*" + column + ")*"; // 多列正则表达式 匹配如
															// product
															// p,category
															// c,warehouse w
		// 一列的正则表达式匹配如a.product p
		String ownerenable = "((\\w+\\.){0,1}\\w+\\s*(\\w+\\s*){0,1})";
		// 多列正则表达式匹配如a.product p,a.category c,b.warehouse w
		String ownerenables = ownerenable + "(,\\s*" + ownerenable + ")*";
		String from = "FROM\\s+" + columns;
		// 条件的正则表达式匹配如a=b或a is b..
		String condition = "(\\w+\\.){0,1}\\w+\\s*(=|LIKE|IS)\\s*'?(\\w+\\.){0,1}[\\w%]+'?";
		// 多个条件匹配如a=b and c like 'r%' or d is null
		String conditions = condition + "(\\s+(AND|OR)\\s*" + condition + "\\s*)*";
		String where = "(WHERE\\s+" + conditions + "){0,1}";
		String pattern = "SELECT\\s+(\\*|" + ownerenables + "\\s+" + from + ")\\s+" + where + "\\s*"; // 匹配最终sql的正则表达式
		// System.out.println(pattern);// 输出正则表达式

		boolean bRet = span.matches(pattern);// 是否比配
		return bRet;
	}

	// 通过Url获取主机名，port, warehouse
	public static boolean isMatch(String strUrl, String regExp, Matcher[] retMatch) {
		// 1.通过Url获取主机名，port, warehouse
		// String regex="hdfs:\\/\\/(.*?):(\\d+)(\\/.*)";
		// 2.通过Url获取主机名，port
		// String regex="hdfs:\\/\\/(.*?):(\\d+)";
		if (strUrl == null || strUrl.isEmpty()) {
			return false;
		}

		if (regExp == null || regExp.isEmpty()) {
			throw new RQException("isMatch regExp is empty");
		}

		Pattern p = Pattern.compile(regExp);
		retMatch[0] = p.matcher(strUrl);

		return retMatch[0].find();
	}

	// 输出结果
	public static void doPrint(List<Object> result) {
		for (Object row : result) {
			System.out.println(row.toString());
		}
	}

	public static List<List<Object>> resultsConvertDList(List<Object> result) {
		if (result == null)
			return null;
		if (result.size() == 0)
			return null;

		List<List<Object>> lls = new ArrayList<List<Object>>();
		for (Object row : result) {
			String[] sourceStrArray = row.toString().split("\t");
			List list = Arrays.asList(sourceStrArray);
			lls.add(list);
		}

		return lls;
	}

	public static void testPrintTable(Table table) {
		if (table == null)
			return;
		System.out.println("size = " + table.length());

		DataStruct ds = table.dataStruct();
		String[] fields = ds.getFieldNames();
		int i = 0;
		// print colNames;
		for (i = 0; i < fields.length; i++) {
			System.out.print(fields[i] + "\t");
		}
		System.out.println();
		// print tableData
		for (i = 0; i < table.length(); i++) {
			BaseRecord rc = table.getRecord(i + 1);
			Object[] objs = rc.getFieldValues();
			for (Object o : objs) {
				System.out.printf(o + "\t");
			}
			System.out.println();
		}
	}

	public static Table toTable(CellSet cellSet) {
		if (cellSet == null) {
			return null;
		}
		
		Object[] objs = null;
		// header
		String colNames[] = getColumnNames(cellSet);		
		Table table = new Table(colNames);
		
		// data
		int idx = 0;
		Cell cell = null;
		if (cellSet.getAxes().size()==1) { //only Axis1
			String line = null;        	          
            for (Position row : cellSet.getAxes().get(0)) {
            	idx = 0;
            	line = "";
	        	objs = new Object[colNames.length];
	    		List<Member> members = row.getMembers();
	    		for (Member m : members) {
	    			if (line.isEmpty()) {
	    				line = m.getCaption();
	    			} else {
	    				line += "_" + m.getCaption();
	    			}
	    		}
	    		objs[0] = line;
                cell = cellSet.getCell(row);
                objs[1] = cell.getValue();
                table.newLast(objs); 
            }
		}else{
			for (Position row : cellSet.getAxes().get(1)) {
	        	idx = 0;
	        	objs = new Object[colNames.length];
	        	for (Member member : row.getMembers()) {
					objs[idx++] = member.getName();
				}
	            for (Position column : cellSet.getAxes().get(0)) {
	                cell = cellSet.getCell(column, row);
	                objs[idx++] = cell.getValue();
	            }
	            table.newLast(objs);           
	        }
		}
		
		return table;
	}
	
	public static Table toTable(CellSet cellSet, String[] colNames) {
		if (cellSet == null) {
			return null;
		}

		int idx = 0;
		Cell cell = null;
		Object[] objs = null;		
		Table table = new Table(colNames);
		if (cellSet.getAxes().size() == 1) {
			String line = null;
			for (Position row : cellSet.getAxes().get(0)) {
				line = "";
				objs = new Object[colNames.length];
				List<Member> members = row.getMembers();
	    		for (Member m : members) {
	    			if (line.isEmpty()) {
	    				line = m.getCaption();
	    			} else {
	    				line += "_" + m.getCaption();
	    			}
	    		}
				cell = cellSet.getCell(row);
				objs[0] = line;
				objs[1] = cell.getValue();
				table.newLast(objs);
			}
		} else {
			for (Position row : cellSet.getAxes().get(1)) {
				idx = 0;
				objs = new Object[colNames.length];
				for (Member member : row.getMembers()) {
					objs[idx++] = member.getName();
				}

				for (Position column : cellSet.getAxes().get(0)) {
					cell = cellSet.getCell(column, row);
					objs[idx++] = cell.getValue();
				}
				table.newLast(objs);
			}
		}

		return table;
	}

	public static String[] getColumnNames(CellSet cellSet) {
		if (cellSet.getAxes().size() == 1) {
			return new String[]{" ", "value"};
		}
		
		List<String> colNames = new ArrayList<String>();
		if (cellSet.getAxes().size() == 2) {
			for (Position row : cellSet.getAxes().get(1)) {
				String regExp = "([\\s\\S]*)\\[(.*)\\]\\.[\\[all.*?\\] | &\\[\\s\\S]*]$";
				String name = "";
				Pattern p = Pattern.compile(regExp);
				for (Member member : row.getMembers()) {
					Dimension dm = member.getDimension();
					Member mem = member.getParentMember();
					if (mem != null) {
						name = mem.getUniqueName();
						Matcher m = p.matcher(name.toLowerCase());
						boolean bMatch = false;
						if (m.find()) {
							if (m.groupCount() == 2) { // 索引以1开始
								colNames.add(m.group(2));
								bMatch = true;
							}
						}
						if (!bMatch) {
							colNames.add("");
						}
					} else if (dm != null) {
						colNames.add(dm.getCaption());
					} else {
						colNames.add("");
					}
				}
				break;
			}
		}
		String colName = "";
		for (Position pos : cellSet.getAxes().get(0)) {
			colName = "";
			List<Member> members = pos.getMembers();
			for (Member m : members) {
				if (colName.isEmpty()) {
					colName = m.getCaption();
				} else {
					colName += "_" + m.getCaption();
				}
			}
			colNames.add(colName);
		}
		
		String[] strings = new String[colNames.size()];
		colNames.toArray(strings);

		return strings;
	}
}
