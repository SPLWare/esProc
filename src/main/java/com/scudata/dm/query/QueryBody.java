package com.scudata.dm.query;

import java.util.ArrayList;

import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.query.Select.Exp;
import com.scudata.expression.Expression;

//from的对象
abstract class QueryBody {
	protected Select select;
	protected String aliasName;
	protected String joinFieldName; // 做关联后表所对应的字段名
	
	public String getAliasName() {
		return aliasName;
	}

	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}

	public String getJoinFieldName() {
		return joinFieldName;
	}

	public void setJoinFieldName(String joinFieldName) {
		this.joinFieldName = joinFieldName;
	}
	
	int getSelectLevel() {
		return select.getLevel();
	}
	
	// 取表的数据结构
	public abstract DataStruct getDataStruct();
	
	public int getFileAttributeCount() {
		return 0;
	}
	
	// 查找字段对应的表
	public abstract QueryBody getQueryBody(String tableName, String fieldName);
	
	public abstract QueryBody getQueryBody(String tableName);
	
	public void getAllJoinTables(ArrayList<QueryBody> tableList) {
		tableList.add(this);
	}

	/**
	 * 取数据，根据实际情况返回序表或者游标
	 * @return
	 */
	public Object getData() {
		return getData(null);
	}
	
	public abstract Object getData(Exp where);
	
	// 过滤取数
	public Object select(String where) {
		Object data = getData();
		if (where == null) {
			return data;
		}
		
		Context ctx = select.getContext();
		Expression exp = new Expression(select.getCellSet(), ctx, where);
		
		if (data instanceof Sequence) {
			return ((Sequence)data).select(exp, null, ctx);
		} else if (data instanceof ICursor) {
			ICursor cs = (ICursor)data;
			return cs.select(null, exp, null, ctx);
		} else {
			return data;
		}
	}
	
	public String toSPL() {
		throw new RuntimeException();
	}
}
