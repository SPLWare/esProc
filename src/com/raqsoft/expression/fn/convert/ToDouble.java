package com.raqsoft.expression.fn.convert;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;

/**
 * 将字符串或数字转换成64位的双精度浮点数
 * @author runqian
 *
 */
public class ToDouble extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("float" + mm.getMessage("function.invalidParam"));
		}
		
		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof Double) {
			return result;
		} else if (result instanceof Number) {
			return ((Number)result).doubleValue();
		} else if (result instanceof String) {
			try {
				return new Double((String)result);
			} catch (NumberFormatException e) {
				return null;
			}
		} else if (result == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("float" + mm.getMessage("function.paramTypeError"));
		}
	}
}
