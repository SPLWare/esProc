package com.scudata.expression.fn.string;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * mid(s, start{, len}) 返回字符串s中从start位置起长度为len的子串。
 * @author runqian
 *
 */
public class Mid extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		if (size != 2 && size != 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam"));
		}

		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = sub1.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
		}

		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (!(result2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
		}

		int begin = ((Number)result2).intValue() - 1;
		if (begin < 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam") + result2 + " < 1");
		}

		Number lenObj = null;
		if (size > 2) {
			IParam sub3 = param.getSub(2);
			if (sub3 != null) {
				Object result3 = sub3.getLeafExpression().calculate(ctx);
				if (!(result3 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("mid" + mm.getMessage("function.paramTypeError"));
				}
				
				lenObj = (Number)result3;
			}
		}

		String str = (String)result1;
		int end = str.length();
		if (lenObj != null) {
			end = lenObj.intValue() + begin;
		}

		int len = str.length();
		if (begin >= len) {
			return "";
		} else if (end > len) {
			return str.substring(begin, len);
		} else if (end < begin) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mid" + mm.getMessage("function.invalidParam"));
		} else {
			return str.substring(begin, end);
		}
	}
}
