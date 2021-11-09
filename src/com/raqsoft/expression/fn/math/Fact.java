package com.raqsoft.expression.fn.math;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Expression;
import com.raqsoft.dm.Context;

public class Fact extends Function {

	public static long fact(long n) {
		if (n == 1) {
			return 1;
		}
		long result=1;
		for(long i=1;i<=n;i++){
			result*=i;
		}
		return result;
	}

	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fact" +  mm.getMessage("function.invalidParam"));
		}
		Expression param1 = param.getLeafExpression();
		Object result1 = param1.calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fact" + mm.getMessage("function.paramTypeError"));
		}
		
		return new Long(fact(((Number)result1).longValue()));
	}

}
