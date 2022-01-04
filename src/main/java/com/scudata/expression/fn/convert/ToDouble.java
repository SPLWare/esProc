package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

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
