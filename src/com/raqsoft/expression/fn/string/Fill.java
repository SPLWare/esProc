package com.raqsoft.expression.fn.string;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * fill(s,n) 获取n个s拼成的字符串
 * @author runqian
 *
 */
public class Fill extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fill" + mm.getMessage("function.missingParam"));
		}

		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fill" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fill" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = sub1.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
		}

		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (!(result2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fill" + mm.getMessage("function.paramTypeError"));
		}
		
		int n = ((Number)result2).intValue();
		if (n < 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("fill" + mm.getMessage("function.valueNoSmall"));
		}
		
		String str = (String) result1;
		int len = str.length();
		StringBuffer sb = new StringBuffer(n * len);
		for (int i = 0; i < n; i++) {
			sb.append(str);
		}
		
		return sb.toString();
	}
}
