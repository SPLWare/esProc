package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * ifstring(x) 判定参数x是否为字符串类型
 * @author runqian
 *
 */
public class IfString extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifstring" + mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		return (obj instanceof String) ? Boolean.TRUE : Boolean.FALSE;
	}
}
