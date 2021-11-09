package com.raqsoft.expression.fn.string;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * right(s,n) 获得字符串s右边长度为n的子串。当n<0时，n的数值为string串的长度加n值。
 * @author runqian
 *
 */
public class Right extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("right" + mm.getMessage("function.missingParam"));
		}

		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("right" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("right" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = sub1.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("right" +  mm.getMessage("function.paramTypeError"));
		}
		
		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (!(result2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("right" +  mm.getMessage("function.paramTypeError"));
		}
		
		String str = (String) result1;
		int len = str.length();
		int n = ((Number)result2).intValue();
		if (n < 0) {
			n = -n;
			if (n >= len) {
				return "";
				//MessageManager mm = EngineMessage.get();
				//throw new RQException("right" + mm.getMessage("function.valueNoSmall"));
			} else {
				return str.substring(n);
			}
		} else if (n >= len) {
			return result1;
		} else {
			return str.substring(len - n);
		}
	}
}
