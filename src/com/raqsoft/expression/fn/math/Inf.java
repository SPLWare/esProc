package com.raqsoft.expression.fn.math;

import com.raqsoft.expression.Function;
import com.raqsoft.dm.Context;

public class Inf extends Function {

	public Object calculate(Context ctx) {
		return new Double(Double.POSITIVE_INFINITY);
	}
}
