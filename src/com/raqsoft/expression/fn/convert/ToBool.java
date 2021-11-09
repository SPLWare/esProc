package com.raqsoft.expression.fn.convert;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;

/**
 * bool(expression) 将表达式expression的数据类型转换为布尔型。
 * 转换规则：当参数值为null、字符串"false"(大小敏感)、布尔值false时返回false，否则返回true。
 * @author runqian
 *
 */
public class ToBool extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("bool" + mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Boolean) {
			return obj;
		} else if (obj instanceof String) {
			if (((String)obj).equals("false")) {
				return Boolean.FALSE;
			} else {
				return Boolean.TRUE;
			}
		} else if (obj == null)  {
			return Boolean.FALSE;
		} else {
			return Boolean.TRUE;
		}
	}
}
