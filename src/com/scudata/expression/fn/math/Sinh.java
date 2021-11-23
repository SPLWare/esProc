package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 返回某一数字的双曲正弦值sinh(z)=(e^z-e^(-z))/2
 * @author yanjing
 *
 */
public class Sinh extends Function {

	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sinh" + mm.getMessage("function.invalidParam"));
		}
		Expression param1 = param.getLeafExpression();
		Object result1 = param1.calculate(ctx);
		if (result1 == null) {
			return result1;
		}
		if (! (result1 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sinh" + mm.getMessage("function.paramTypeError"));
		}
		double z=Variant.doubleValue(result1);
		return new Double(Math.sinh(z));
	}

}
