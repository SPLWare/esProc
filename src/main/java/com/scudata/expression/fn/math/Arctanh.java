package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

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
