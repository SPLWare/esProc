package com.raqsoft.expression;

import com.raqsoft.dm.Record;

/**
 * 记录成员函数基类
 * r.f()
 * @author RunQian
 *
 */
public abstract class RecordFunction extends MemberFunction {
	protected Record srcRecord; // 源记录
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof Record;
	}
	
	public void setDotLeftObject(Object obj) {
		srcRecord = (Record)obj;
	}
}
