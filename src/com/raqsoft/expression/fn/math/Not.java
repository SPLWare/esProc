package com.raqsoft.expression.fn.math;

import com.raqsoft.expression.Function;
import com.raqsoft.dm.Context;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;

/**
 * 求参数的按位取反
 * @author yanjing
 *
 */
public class Not extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("not" + mm.getMessage("function.missingParam"));
		}
		
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof BigDecimal) {
			BigInteger bi = ((BigDecimal)obj).toBigInteger().not();
			return new BigDecimal(bi);
		} else if (obj instanceof BigInteger) {
			BigInteger bi = ((BigInteger)obj).not();
			return new BigDecimal(bi);
		} else if (obj instanceof Number) {
			return ~((Number)obj).longValue();
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("not" + mm.getMessage("function.paramTypeError"));
		}
	}
}
