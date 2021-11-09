package com.raqsoft.expression.fn.math;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.dm.Context;
import com.raqsoft.util.Variant;

public class Pow extends Function {

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null) {
			throw new RQException("power" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object val = param.getLeafExpression().calculate(ctx);
			return Variant.square(val);
		}
		if (param.getSubSize() != 2) {
			throw new RQException("power" + mm.getMessage("function.invalidParam"));
		}
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			throw new RQException("power" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = sub1.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof Number)) {
			throw new RQException("power" + mm.getMessage("function.paramTypeError"));
		}

		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (result2 == null) {
			return null;
		} else if (!(result2 instanceof Number)) {
			throw new RQException("power" + mm.getMessage("function.paramTypeError"));
		}
		
		return new Double(Math.pow(Variant.doubleValue(result1),
								   Variant.doubleValue(result2)));
	}

}
