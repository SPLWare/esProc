package com.scudata.dm.query;

import java.util.List;

import com.scudata.dm.query.Select.Exp;
import com.scudata.dm.query.Select.FieldNode;

class And {
	private Exp exp;
	private List<FieldNode> fieldList;
	
	public And(Exp exp, List<FieldNode> fieldList) {
		this.exp = exp;
		this.fieldList = fieldList;
	}
	
	public boolean containTable(QueryBody table) {
		if (fieldList != null) {
			for (FieldNode field : fieldList) {
				if (field.getTable() == table) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean isSingleTable(QueryBody table) {
		if (fieldList == null || fieldList.size() == 0) {
			return false;
		}
		
		for (FieldNode field : fieldList) {
			if (field.getTable() != table) {
				return false;
			}
		}
		
		return true;
	}
	
	public boolean isTable(QueryBody leftTable, QueryBody rightTable) {
		if (fieldList != null) {
			for (FieldNode field : fieldList) {
				QueryBody table = field.getTable();
				if (table != leftTable && table != rightTable) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public boolean isTable(List<QueryBody> tableList, int lastTable) {
		if (fieldList != null) {
			for (FieldNode field : fieldList) {
				QueryBody table = field.getTable();
				int index = tableList.indexOf(table);
				if (index < 0 || index > lastTable) {
					return false;
				}
			}
		}
		
		return true;
	}

	public Exp getExp() {
		return exp;
	}
}
