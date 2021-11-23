package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * ifnumber(Exp) 判定参数Exp是否为数值类型
 * @author runqian
 *
 */
public class IfNumber extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifnumber" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = param.getLeafExpression().calculate(ctx);
		if (result1 instanceof Number) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
}
