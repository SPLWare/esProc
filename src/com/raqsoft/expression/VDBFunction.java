package com.raqsoft.expression;

import com.raqsoft.vdb.VDB;

/**
 * 交易库成员函数基类
 * v.f()
 * @author RunQian
 *
 */
public abstract class VDBFunction extends MemberFunction {
	protected VDB vdb; // 源数据库
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof VDB;
	}

	public void setDotLeftObject(Object obj) {
		vdb = (VDB)obj;
	}
}
