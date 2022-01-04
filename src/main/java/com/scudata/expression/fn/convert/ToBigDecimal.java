package com.scudata.expression.fn.convert;

import java.math.BigDecimal;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

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
