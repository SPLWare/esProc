package com.raqsoft.expression;

import com.raqsoft.parallel.ClusterTableMetaData;

/**
 * 集群组表函数基类
 * T.f()
 * @author RunQian
 *
 */
public abstract class ClusterTableMetaDataFunction extends MemberFunction {
	protected ClusterTableMetaData table;
	
	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof ClusterTableMetaData;
	}

	public void setDotLeftObject(Object obj) {
		table = (ClusterTableMetaData)obj;
	}
}