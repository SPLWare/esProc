package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

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
