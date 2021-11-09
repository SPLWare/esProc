package com.raqsoft.expression.mfn.vdb;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.VSFunction;

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
