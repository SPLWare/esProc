package com.raqsoft.expression.mfn.cluster;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.ClusterFileFunction;

/**
 * 打开集群组表
 * f.open()
 * @author RunQian
 *
 */
public class Open extends ClusterFileFunction {
	public Object calculate(Context ctx) {
		return file.openGroupTable(ctx);
	}
}
