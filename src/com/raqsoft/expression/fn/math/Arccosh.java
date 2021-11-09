package com.raqsoft.expression.fn.math;

import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;

/**
 * 返回某一数字的反双曲余弦值acosh(z)=ln(z+sqrt(z*z-1))
 * @author yanjing
 *
 */
public class Arccosh extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("acosh" + mm.getMessage("function.invalidParam"));
		}
		
		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof Number) {
			double z = Variant.doubleValue(result);
			return new Double(Math.log(z+Math.sqrt(z*z-1)));
		} else if (result == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("acosh" + mm.getMessage("function.paramTypeError"));
		}
	}
}
