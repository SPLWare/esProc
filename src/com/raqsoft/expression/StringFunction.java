package com.raqsoft.expression;

/**
 * 字符串成员函数基类
 * S.f()
 * @author RunQian
 *
 */
public abstract class StringFunction extends MemberFunction {
	protected String srcStr; // 源串
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof String;
	}
	
	public void setDotLeftObject(Object obj) {
		srcStr = (String)obj;
	}
}