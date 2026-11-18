package com.scudata.expression.mfn.dw;

import com.scudata.dm.Context;
import com.scudata.expression.PhyTableFunction;

/**
 * 为了NLP兼容打开的组表再次调用open函数
 * @author RunQian
 *
 */
public class Open extends PhyTableFunction {
	public Object calculate(Context ctx) {
		return table;
	}
}
