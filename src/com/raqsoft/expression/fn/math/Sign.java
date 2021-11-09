package com.raqsoft.expression.fn.math;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Expression;
import com.raqsoft.dm.Context;
import com.raqsoft.util.Variant;

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
