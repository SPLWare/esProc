package com.scudata.expression.fn.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

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
