package com.scudata.expression;

import com.scudata.vdb.IVS;

/**
 * 交易库成员函数基类
 * h.f()
 * @author RunQian
 *
 */
public abstract class VSFunction extends MemberFunction {
	protected IVS vs;

	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof IVS;
	}
	
	public void setDotLeftObject(Object obj) {
		vs = (IVS)obj;
	}
}
