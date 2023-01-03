package com.scudata.expression;

import com.scudata.parallel.ClusterPhyTable;

/**
 * 集群组表函数基类
 * T.f()
 * @author RunQian
 *
 */
public abstract class ClusterPhyTableFunction extends MemberFunction {
	protected ClusterPhyTable table;
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof ClusterPhyTable;
	}

	public void setDotLeftObject(Object obj) {
		table = (ClusterPhyTable)obj;
	}
}