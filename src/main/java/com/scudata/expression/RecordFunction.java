package com.scudata.expression;

import com.scudata.dm.BaseRecord;

/**
 * 记录成员函数基类
 * r.f()
 * @author RunQian
 *
 */
public abstract class RecordFunction extends MemberFunction {
	protected BaseRecord srcRecord; // 源记录
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof BaseRecord;
	}
	
	public void setDotLeftObject(Object obj) {
		srcRecord = (BaseRecord)obj;
	}
	
	/**
	 * 释放节点引用的点操作符左侧的对象
	 */
	public void releaseDotLeftObject() {
		srcRecord = null;
	}
}
