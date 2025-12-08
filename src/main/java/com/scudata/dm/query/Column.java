package com.scudata.dm.query;

import java.util.ArrayList;
import java.util.List;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.DataStruct;
import com.scudata.dm.query.Select.Exp;
import com.scudata.resources.ParseMessage;

class Column {
	private Select select;
	private Exp exp; // 表达式，为null时则表示*选取所有字段
	private String aliasName; // 别名
	private String tableName; // 表名用于T.*
	
	public Column(Select select, Exp exp, String aliasName) {
		this.select = select;
		this.exp = exp;
		if (aliasName == null) {
			this.aliasName = exp.getFieldName();
		} else {
			this.aliasName = aliasName;
		}
	}
	
	public Column(Select select, String tableName) {
		this.select = select;
		this.tableName = tableName;
	}

	public boolean isEquals(String name) {
		return aliasName != null && Select.isEquals(aliasName, name);
	}
	
	public Exp getExp() {
		return exp;
	}

	public void setExp(Exp exp) {
		this.exp = exp;
	}

	public String getAliasName() {
		if (aliasName == null || aliasName.length() == 0) {
			return null;
		} else if (aliasName.charAt(0) == '"') {
			// 双引号变成单引号
			int last = aliasName.length() - 1;
			if (last > 0 && aliasName.charAt(last) == '"') {
				return aliasName.substring(1, last);
			}
		}
		
		return aliasName;
	}

	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}
	
	public String getTableName() {
		return tableName;
	}

	public boolean isAllFields() {
		return exp == null;
	}
	
	// 用于select * from
	public ArrayList<QueryBody> getAllTables() {
		ArrayList<QueryBody> tableList = new ArrayList<QueryBody>();
		if (tableName == null) {
			select.getFrom().getAllJoinTables(tableList);
		} else {
			QueryBody table = select.getFromTable(tableName);
			if (table == null) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(tableName + mm.getMessage("syntax.unknownTable"));
			}
			
			tableList.add(table);
		}
		
		return tableList;
	}
	
	public void getResultField(List<String> nameList) {
		if (exp != null) {
			nameList.add(aliasName);
		} else if (tableName == null) {
			ArrayList<QueryBody> tableList = new ArrayList<QueryBody>();
			select.getFrom().getAllJoinTables(tableList);
			
			for (QueryBody table : tableList) {
				DataStruct ds = table.getDataStruct();
				String []names = ds.getFieldNames();
				for (String name : names) {
					nameList.add(name);
				}
			}
		} else {
			QueryBody table = select.getFromTable(tableName);
			if (table == null) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(tableName + mm.getMessage("syntax.unknownTable"));
			}
			
			DataStruct ds = table.getDataStruct();
			String []names = ds.getFieldNames();
			for (String name : names) {
				nameList.add(name);
			}
		}
	}
	
	public String toSPL() {
		List<Exp> byList = select.getGroupBy();
		if (byList != null) {
			// 判断选出列的表达式是否和分组字段表达式相同，如果是则用序号引用分组值
			int byCount = byList.size();
			for (int i = 0; i < byCount; ++i) {
				Exp byExp = byList.get(i);
				if (byExp.isEquals(exp)) {
					return "#" + (i + 1);
				}
			}
			
			return exp.toSPL(byCount);
		} else if (select.getGatherCount() > 0) {
			// 没有分组字段的全聚合
			return exp.toSPL(0);
		} else {
			return exp.toSPL();
		}
	}
}
