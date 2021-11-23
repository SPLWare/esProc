package com.scudata.parallel;

import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.dm.Table;

/**
 * 节点机序表代理
 * @author WangXiaoJun
 *
 */
public class TableProxy extends IProxy {
	private Table table;
	private int unit;
	
	public TableProxy(Table table) {
		this.table = table;
	}

	public TableProxy(Table table, int unit) {
		this.table = table;
		this.unit = unit;
	}
	
	public void close() {		
	}
	
	public Record getRow(Object key, Context ctx) {
		return (Record) table.findByKey(key, true);
	}
	
	public boolean createIndex(Integer capacity, String opt) {
		if (capacity == null) {
			table.createIndexTable(null);
		} else {
			table.createIndexTable(capacity, opt);
		}
		return true;
	}
	
	public int getUnit() {
		return unit;
	}
	
	public Table getTable() {
		return table;
	}
	
	public void setTable(Table table) {
		this.table = table;
	}
}