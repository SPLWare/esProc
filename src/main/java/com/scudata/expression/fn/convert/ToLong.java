package com.scudata.expression.fn.convert;

import java.util.Date;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 将字符串、数字或日期转换成64位长整数
 * @author runqian
 *
 */
public class ToLong extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("long" + mm.getMessage("function.invalidParam"));
		}

		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof Long) {
			return result;
		} else if (result instanceof Number) {
			return new Long(((Number)result).longValue());
		} else if (result instanceof String) {
			try {
				return Long.parseLong((String)result);
			} catch (NumberFormatException e) {
				return null;
			}
		} else if (result instanceof Date) {
			return new Long(((Date)result).getTime());
		} else if (result == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("long" + mm.getMessage("function.paramTypeError"));
		}
	}
}
