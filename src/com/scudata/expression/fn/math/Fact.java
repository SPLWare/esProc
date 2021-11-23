package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

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
