package com.scudata.expression.fn.math;

import com.scudata.dm.Context;
import com.scudata.expression.Function;

public class Inf extends Function {

	public Object calculate(Context ctx) {
		return new Double(Double.POSITIVE_INFINITY);
	}
}
