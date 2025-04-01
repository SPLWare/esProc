package com.scudata.expression.mfn.vdb;

import com.scudata.dm.Context;
import com.scudata.expression.VDBFunction;

/**
 * 启动事务
 * v.begin()
 * @author RunQian
 *
 */
public class Begin extends VDBFunction {
	public Object calculate(Context ctx) {
		return vdb.begin();
	}
}
