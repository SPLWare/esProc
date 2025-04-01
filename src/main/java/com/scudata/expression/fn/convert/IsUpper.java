package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * isupper(string) 判定字符串string是否全由大写字母构成。
 * 如果string为整数，则作为ascii码，判断其对应的字符是否为大写字母。
 * @author runqian
 *
 */
public class IsUpper extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("isupper" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("isupper" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object result1 = param.getLeafExpression().calculate(ctx);
		if (result1 instanceof String) {
			String str = (String)result1;
			if (str.length() == 0) return Boolean.FALSE;

			for (int i = 0, len = str.length(); i < len; ++i) {
				char c = str.charAt(i);
				if (c < 'A' || c > 'Z') {
					return Boolean.FALSE;
				}
			}

			return Boolean.TRUE;
		} else if (result1 instanceof Number) {
			int c = ((Number)result1).intValue();
			if (c >= 'A' && c <= 'Z') {
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		} else {
			return Boolean.FALSE;
		}
	}
}
