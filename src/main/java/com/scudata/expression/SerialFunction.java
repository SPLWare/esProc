package com.scudata.expression;

import com.scudata.dm.SerialBytes;
import com.scudata.expression.MemberFunction;

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
}
