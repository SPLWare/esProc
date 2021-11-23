package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * isalpha(s) 判定字符串s是否全由字母构成。如果s为整数，则作为ascii码判断对应的字符是否为字母。
 * @author runqian
 *
 */
public class IsAlpha extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("isalpha" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = param.getLeafExpression().calculate(ctx);
		if (result1 instanceof String) {
			String str = (String)result1;
			if (str.length() == 0) return Boolean.FALSE;

			for (int i = 0, len = str.length(); i < len; ++i) {
				char c = str.charAt(i);
				if (c < 'A' || (c > 'Z' && c < 'a') || c > 'z') {
					return Boolean.FALSE;
				}
			}

			return Boolean.TRUE;
		} else if (result1 instanceof Number) {
			int c = ((Number)result1).intValue();
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		} else {
			return Boolean.FALSE;
		}
	}
}
