package com.raqsoft.expression.fn.math;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.Function;
import com.raqsoft.dm.Context;
import com.raqsoft.util.Variant;

/**
 * 返回某一数字的反双曲正切值atanh(z)=(1/2)*ln((1+z)/(1-z))
 * @author yanjing
 *
 */
public class Arctanh extends Function {

	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("atanh" + mm.getMessage("function.invalidParam"));
		}
		Object result1 = param.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return result1;
		}
		if (! (result1 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("atanh" + mm.getMessage("function.paramTypeError"));
		}
		double z=Variant.doubleValue(result1);
		
		return new Double((1.0/2.0)*Math.log((1+z)/(1-z)));
	}

}
