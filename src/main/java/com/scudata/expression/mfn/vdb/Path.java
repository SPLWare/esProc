package com.scudata.expression.mfn.vdb;

import com.scudata.dm.Context;
import com.scudata.expression.VSFunction;

/**
 * 返回当前路径的节值
 * h.path()
 * @author RunQian
 *
 */
public class Path extends VSFunction {
	public Object calculate(Context ctx) {
		return vs.path(option);
	}
}
