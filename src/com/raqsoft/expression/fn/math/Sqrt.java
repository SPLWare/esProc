package com.raqsoft.expression.fn.math;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.dm.Context;

public class Sqrt extends Function {

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null) {
			throw new RQException("sqrt" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Expression param1 = param.getLeafExpression();
			Object result = param1.calculate(ctx);
			if (result instanceof Number) {
				return new Double(Math.sqrt(((Number)result).doubleValue()));
			} else if (result == null) {
				return null;
			} else {
				throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				throw new RQException("sqrt" + mm.getMessage("function.invalidParam"));
			}
			
			Object a = sub0.getLeafExpression().calculate(ctx);
			if (a == null) {
				return null;
			} else if (!(a instanceof Number)) {
				throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
			}
			
			Object b = sub1.getLeafExpression().calculate(ctx);
			if (!(b instanceof Number)) {
				throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
			}
			
			double p = 1 / ((Number)b).doubleValue();
			return new Double(Math.pow(((Number)a).doubleValue(), p));
		} else {
			throw new RQException("sqrt" + mm.getMessage("function.invalidParam"));
		}
	}

}
