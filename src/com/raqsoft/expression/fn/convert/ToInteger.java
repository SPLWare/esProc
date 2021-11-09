package com.raqsoft.expression.fn.convert;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;

/**
 * 类型转换函数，取出给定数值表达式或数值字符串中的整数部分，并把其数据类型转换成32位整数。
 * @author runqian
 *
 */
public class ToInteger extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("int" + mm.getMessage("function.invalidParam"));
		}

		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof Integer) {
			return result;
		} else if (result instanceof Number) {
			return new Integer(((Number)result).intValue());
		} else if (result instanceof String) {
			try {
				return new Double((String)result).intValue();
			} catch (NumberFormatException e) {
				return null;
			}
		} else if (result == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("int" + mm.getMessage("function.paramTypeError"));
		}
	}
}
