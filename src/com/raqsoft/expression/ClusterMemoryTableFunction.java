package com.raqsoft.expression;

import com.raqsoft.parallel.ClusterMemoryTable;

/**
 * 集群内表成员函数基类
 * T.f()
 * @author RunQian
 *
 */
public abstract class ClusterMemoryTableFunction extends MemberFunction {
	protected ClusterMemoryTable table;
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof ClusterMemoryTable;
	}

	public void setDotLeftObject(Object obj) {
		table = (ClusterMemoryTable)obj;
	}
}