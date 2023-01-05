package com.scudata.lib.joinquant.function;

import com.scudata.dm.BaseRecord;
import com.scudata.dm.Table;
import com.scudata.util.Variant;

public class DataType {
		
	static Table toTable(String[] rows, String[] colNames) {
		int len = rows.length;
		if (rows == null || rows.length < 1)
			return null;
		
		Table table = null;
		if (rows[0].startsWith("error:")) {
			String[] cols = new String[] {"Title"};
			table = new Table(cols, cols.length);
			for(String s:rows) {
				table.newLast(new String[] {s});
			}
		}else {
			table = new Table(colNames, len);
			int n = 0;
			for (int i=0; i<rows.length; i++) {
				String[] sourceStrArray = rows[i].split(",", -1);			
				if (sourceStrArray.length==colNames.length) {
					n = 0;
					Object[] objs = new Object[sourceStrArray.length];
					for(String s:sourceStrArray) {
						objs[n++]= Variant.parse(s, false);
					}
					table.newLast(objs);
				}
			}
		}
		return table;
	}
	
	//����Ϊheader
	static Table toTable(String[] rows) {
		if (rows == null || rows.length < 1)
			return null;
		
		Table table = null;
		if (rows[0].startsWith("error:")) {
			String[] colNames = new String[] {"Title"};
			table = new Table(colNames, colNames.length);
			for(String s:rows) {
				table.newLast(new String[] {s});
			}
		}else {
			int n = 0;
			String[] colNames = rows[0].split(",");
			table = new Table(colNames, colNames.length);
			for (int i=1; i<rows.length; i++) {
				String[] sourceStrArray = rows[i].split(",", -1);			
				if (sourceStrArray.length==colNames.length) {
					n = 0;
					Object[] objs = new Object[sourceStrArray.length];
					for(String s:sourceStrArray) {
						objs[n++]= Variant.parse(s, false);
					}
					table.newLast(objs);
				}
//				else {
//					System.out.println(sourceStrArray.length+"=="+colNames.length);
//				}
			}
		}
		return table;
	}
	
	//首行为header
	static Table toTableWithCode(String code, String name, String[] rows) {
		if (rows == null || rows.length < 1)
			return null;
		
		Table table = null;
		if (rows[0].startsWith("error:")) {
			String[] colNames = new String[] {"Title"};
			table = new Table(colNames, colNames.length);
			for(String s:rows) {
				table.newLast(new String[] {s});
			}
		}else {
			int n = 0;
			String[] colNames = rows[0].split(",");
			String[] cols = new String[colNames.length+2];
			cols[0] = "code";
			cols[1] = "name";
			for(int i=0; i<colNames.length; i++) {
				cols[i+2]=colNames[i];
			}
			table = new Table(cols, cols.length);
			for (int i=1; i<rows.length; i++) {
				String[] sourceStrArray = rows[i].split(",", -1);			
				if (sourceStrArray.length==colNames.length) {
					n = 2;
					Object[] objs = new Object[sourceStrArray.length+2];
					objs[0] = code;
					objs[1] = name;
					for(String s:sourceStrArray) {
						objs[n++]= Variant.parse(s, false);
					}
					table.newLast(objs);
				}
			}
		}
		return table;
	}
		
	//首行为header
	static Table toTableOfString(String[] rows) {
		if (rows == null || rows.length < 1)
			return null;

		Table table = null;
		if (rows[0].startsWith("error:")) {
			String[] colNames = new String[] {"Title"};
			table = new Table(colNames, colNames.length);
			for(String s:rows) {
				table.newLast(new String[] {s});
			}
		}else {
			String[] colNames = rows[0].split(",");
			table = new Table(colNames, colNames.length);
			for (int i=1; i<rows.length; i++) {
				String[] sourceStrArray = rows[i].split(",", -1);			
				if (sourceStrArray.length==colNames.length) {
					table.newLast(sourceStrArray);
				}
			}
		}
		return table;
	}
	
	//首行为header
	static Table toTableFromRecord(BaseRecord r) {
		if (r == null )
			return null;

		String[] colNames = r.getFieldNames();
		Table table = new Table(colNames, colNames.length);
		table.newLast(r.getFieldValues());

		return table;
	}
}
