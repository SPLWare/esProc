package com.scudata.expression;

import com.scudata.dm.op.Operable;

/**
 * 可附加操作对象成员函数基类
 * cs.f()，ch.f()
 * @author RunQian
 *
 */
public abstract class OperableFunction extends MemberFunction {
	protected Operable operable;

	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof Operable;
	}
	
	public void setDotLeftObject(Object obj) {
		operable = (Operable)obj;
	}
}