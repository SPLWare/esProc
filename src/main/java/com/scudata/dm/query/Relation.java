package com.scudata.dm.query;

// 表、表join、表union等
abstract class Relation extends QueryBody {
	protected QueryBody left;
	protected QueryBody right;
	
	public QueryBody getLeft() {
		return left;
	}
	
	public void setLeft(QueryBody left) {
		this.left = left;
	}
	
	public QueryBody getRight() {
		return right;
	}
	
	public void setRight(QueryBody right) {
		this.right = right;
	}

	public QueryBody getQueryBody(String tableName) {
		QueryBody query = left.getQueryBody(tableName);
		if (query != null) {
			return query;
		} else {
			return right.getQueryBody(tableName);
		}
	}
	
	public QueryBody getQueryBody(String tableName, String fieldName) {
		QueryBody query = left.getQueryBody(tableName, fieldName);
		if (query != null) {
			return query;
		} else {
			return right.getQueryBody(tableName, fieldName);
		}
	}
}
