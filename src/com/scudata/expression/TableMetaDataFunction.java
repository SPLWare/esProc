package com.scudata.expression;

import com.scudata.dw.ITableMetaData;

/**
 * 组表成员函数基类
 * @author RunQian
 *
 */
public abstract class TableMetaDataFunction extends MemberFunction {
	protected ITableMetaData table;
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof ITableMetaData;
	}

	public void setDotLeftObject(Object obj) {
		table = (ITableMetaData)obj;
	}
}