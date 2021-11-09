package com.raqsoft.expression.fn.string;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * left(string,n) 获得源字符串string左边的子串，其长度为n。当n<0时，n的数值为string串的长度加n值。
 * @author runqian
 *
 */
public class Left extends Function {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("left" + mm.getMessage("function.missingParam"));
		}

		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("left" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("left" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = sub1.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("left" + mm.getMessage("function.paramTypeError"));
		}

		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (!(result2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("left" + mm.getMessage("function.paramTypeError"));
		}
		
		String str = (String) result1;
		int n = ((Number)result2).intValue();
		if (n < 0) {
			n += str.length();
			if (n <= 0) {
				return "";
				//MessageManager mm = EngineMessage.get();
				//throw new RQException("left" + mm.getMessage("function.valueNoSmall"));
			} else {
				return str.substring(0, n);
			}
		} else if (n >= str.length()) {
			return result1;
		} else {
			return str.substring(0, n);
		}
	}
}
