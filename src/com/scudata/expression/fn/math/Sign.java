package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

public class Sign extends Function {

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null || !param.isLeaf()) {
			throw new RQException("sign" + mm.getMessage("function.invalidParam"));
		}

		Expression param1 = param.getLeafExpression();
		Object result1 = param1.calculate(ctx);
		if (result1 == null) {
			return result1;
		}
		if (! (result1 instanceof Number)) {
			throw new RQException("sign" + mm.getMessage("function.paramTypeError"));
		}

		return new Integer(Variant.compare(result1, new Integer(0), true));
	}
}
