package com.scudata.expression;

import com.scudata.dw.MemoryTable;

/**
 * 内表成员函数基类
 * T.f()
 * @author RunQian
 *
 */
public abstract class MemoryTableFunction extends MemberFunction {
	protected MemoryTable table;
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof MemoryTable;
	}

	public void setDotLeftObject(Object obj) {
		table = (MemoryTable)obj;
	}
}