package com.raqsoft.expression.fn.convert;

import java.math.BigDecimal;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 将字符串或数值型的数值转换成大浮点数
 * decimal(stringExp) 参数stringExp必须是由数字和小数点组成的字符串。
 * decimal(numberExp) 参数numberExp只能少于等于64位，超过64位就要用字符串stringExp 代替numberExp。
 * @author runqian
 *
 */
public class ToBigDecimal extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("decimal" + mm.getMessage("function.invalidParam"));
		}
		
		Object result = param.getLeafExpression().calculate(ctx);
		if (result == null) {
			return null;
		} else if (result instanceof String) {
			try {
				return new BigDecimal((String)result);
			} catch (NumberFormatException e) {
				return null;
			}
		} else {
			return Variant.toBigDecimal(result);
		}
	}
}
