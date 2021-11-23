package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 返回某一数字的反双曲正弦值asinh(z)=ln(z+sqrt(z*z+1))
 * @author yanjing
 *
 */
public class Arcsinh extends Function {

	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("asinh" + mm.getMessage("function.invalidParam"));
		}
		Object result1 = param.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return result1;
		}
		if (! (result1 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("asinh" + mm.getMessage("function.paramTypeError"));
		}
		double z=Variant.doubleValue(result1);
		return new Double(Math.log(z+Math.sqrt(z*z+1)));
	}

}
