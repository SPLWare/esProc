package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

public class Arccos extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("acos" + mm.getMessage("function.invalidParam"));
		}

		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof Number) {
			return new Double(Math.acos(((Number)result).doubleValue()));
		} else if (result == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("acos" + mm.getMessage("function.paramTypeError"));
		}
	}
}
