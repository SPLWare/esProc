package com.scudata.expression.fn.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

public class Sign extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sign" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sign" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Integer) {
			int cmp = Integer.compare(((Integer)obj).intValue(), 0);
			return ObjectCache.getInteger(cmp);
		} else if (obj instanceof Long) {
			int cmp = Long.compare(((Long)obj).longValue(), 0L);
			return ObjectCache.getInteger(cmp);
		} else if (obj instanceof Double) {
			int cmp = Double.compare(((Double)obj).doubleValue(), 0.0);
			return ObjectCache.getInteger(cmp);
		} else if (obj instanceof BigDecimal) {
			BigDecimal bd = (BigDecimal)obj;
			int cmp = bd.compareTo(BigDecimal.ZERO);
			return ObjectCache.getInteger(cmp);
		} else if (obj instanceof BigInteger) {
			BigInteger bi = (BigInteger)obj;
			int cmp = bi.compareTo(BigInteger.ZERO);
			return ObjectCache.getInteger(cmp);
		} else if (obj instanceof Number) {
			int cmp = Integer.compare(((Number)obj).intValue(), 0);
			return ObjectCache.getInteger(cmp);
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sign" + mm.getMessage("function.paramTypeError"));
		}
	}
}
