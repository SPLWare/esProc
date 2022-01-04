package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 将数字转为中文数字写法。
 * @author runqian
 *
 */
public class ToChinese extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("chn" + mm.getMessage("function.invalidParam"));
		}

		Object result = param.getLeafExpression().calculate(ctx);
		if (result instanceof Number) {
			boolean abbreviate = false, uppercase = false, rmb = false;
			if (option != null) {
				if (option.indexOf('a') != -1) abbreviate = true;
				if (option.indexOf('u') != -1) uppercase = true;
				if (option.indexOf('b') != -1) rmb = true;
			}
	
			if (rmb) {
				double d = ((Number)result).doubleValue();
				return StringUtils.toRMB(d, abbreviate, uppercase);
			} else {
				long l = ((Number)result).longValue();
				return StringUtils.toChinese(l, abbreviate, uppercase);
			}
		} else if (result == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("chn" + mm.getMessage("function.paramTypeError"));
		}
	}
}
