package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

public class Pi extends Function {
	static Double pi = new Double(Math.PI);

	public Object calculate(Context ctx) {
		if (param == null) {
			return pi;
		}
		
		if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pi" + mm.getMessage("function.invalidParam"));
		}
		
		Object result1 = param.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return result1;
		}
		if (! (result1 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pi" + mm.getMessage("function.paramTypeError"));
		}
		return new Double(Variant.doubleValue(result1) * Math.PI);
	}

}
