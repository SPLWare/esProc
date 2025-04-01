package com.scudata.expression;

import com.scudata.dm.Context;
import com.scudata.dm.Param;
import com.scudata.util.Variant;

/**
 * ~~迭代变量
 * @author RunQian
 *
 */
public class IterateParam extends Node {	
	public Object calculate(Context ctx) {
		return ctx.getIterateParam().getValue();
	}

	public Object assign(Object value, Context ctx) {
		ctx.getIterateParam().setValue(value);
		return value;
	}
	
	public Object addAssign(Object value, Context ctx) {
		Param param = ctx.getIterateParam();
		Object result = Variant.add(param.getValue(), value);
		param.setValue(result);
		return result;
	}
}
