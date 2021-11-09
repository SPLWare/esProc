package com.raqsoft.expression.fn.math;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.Function;
import com.raqsoft.dm.Context;
import com.raqsoft.util.Variant;

public class Pi
	extends Function {
	static Double pi = new Double(Math.PI);

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null) {
			return pi;
		}
		if (!param.isLeaf()) {
			throw new RQException("pi" + mm.getMessage("function.invalidParam"));
		}
		Object result1 = param.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return result1;
		}
		if (! (result1 instanceof Number)) {
			throw new RQException("pi" + mm.getMessage("function.paramTypeError"));
		}
		return new Double(Variant.doubleValue(result1) * Math.PI);
	}

}
