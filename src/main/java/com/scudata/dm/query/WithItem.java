package com.scudata.dm.query;

import java.util.List;

import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.expression.Expression;

class WithItem {
	private String name;
	private List<String> columnNames;
	private QueryBody query;
	private Sequence data;
	
	public WithItem(String name, List<String> columnNames, QueryBody query) {
		this.name = name;
		this.columnNames = columnNames;
		this.query = query;
	}
	
	public boolean equals(String s) {
		return name.equalsIgnoreCase(s);
	}
	
	public Sequence getData() {
		if (data == null) {
			Object value = query.getData();
			if (value instanceof ICursor) {
				data = ((ICursor)value).fetch();
			} else {
				data = (Sequence)value;
			}
			
			if (columnNames != null && columnNames.size() > 0) {
				int fcount = columnNames.size();
				String []names = new String[fcount];
				columnNames.toArray(names);
				
				if (data == null || data.length() == 0) {
					data = new Table(names);
					return data;
				}
				
				DataStruct ds = data.dataStruct();
				if (!ds.isCompatible(names)) {
					Context ctx = new Context();
					Expression []exps = new Expression[fcount];
					for (int f = 0; f < fcount; ++f) {
						exps[f] = new Expression(ctx, "#" + (f + 1));
					}
					
					data = data.newTable(names, exps, ctx);
				}
			}
		}
		
		return data;
	}

}
