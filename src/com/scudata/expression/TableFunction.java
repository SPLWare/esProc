package com.scudata.expression;

import com.scudata.dm.Table;

/**
 * 序表成员函数基类
 * T.f()
 * @author RunQian
 *
 */
public abstract class TableFunction extends MemberFunction {
	protected Table srcTable; // 源序表
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof Table;
	}
	
	public void setDotLeftObject(Object obj) {
		srcTable = (Table)obj;
	}
}
