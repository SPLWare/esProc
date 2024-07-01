package com.scudata.expression;

import com.scudata.dm.SerialBytes;

/**
 * 排号成员函数基类
 * k.f()
 * @author RunQian
 *
 */
public abstract class SerialFunction extends MemberFunction {
	protected SerialBytes sb; // 排号

	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof SerialBytes;
	}
	
	public void setDotLeftObject(Object obj) {
		sb = (SerialBytes)obj;
	}
	
	/**
	 * 释放节点引用的点操作符左侧的对象
	 */
	public void releaseDotLeftObject() {
		sb = null;
	}
}
