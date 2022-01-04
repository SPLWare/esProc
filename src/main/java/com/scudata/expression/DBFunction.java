package com.scudata.expression;

import com.scudata.dm.DBObject;

/**
 * 数据库成员函数基类
 * db.f()
 * @author RunQian
 *
 */
public abstract class DBFunction extends MemberFunction {
	protected DBObject db;

	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof DBObject;
	}
	
	public void setDotLeftObject(Object obj) {
		db = (DBObject)obj;
	}
}