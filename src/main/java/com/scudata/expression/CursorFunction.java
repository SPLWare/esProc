package com.scudata.expression;

import com.scudata.dm.cursor.ICursor;

/**
 * 游标成员函数基类
 * cs.f()
 * @author RunQian
 *
 */
public abstract class CursorFunction extends MemberFunction {
	protected ICursor cursor;

	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof ICursor;
	}
	
	public void setDotLeftObject(Object obj) {
		cursor = (ICursor)obj;
	}
}