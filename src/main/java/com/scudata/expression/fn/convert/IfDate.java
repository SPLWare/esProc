package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * ifdate(exp) 判定参数exp是否为日期型或日期时间类型
 * @author runqian
 *
 */
public class IfDate extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifdate" + mm.getMessage("function.invalidParam"));
		}

		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof java.sql.Time) {
			return Boolean.FALSE;
		} else if (result instanceof java.util.Date) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
}
