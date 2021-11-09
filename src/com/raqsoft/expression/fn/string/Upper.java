package com.raqsoft.expression.fn.string;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.common.Sentence;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;

/**
 * 把字符串转成大写
 * @author runqian
 *
 */
public class Upper extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("upper" + mm.getMessage("function.invalidParam"));
		}
		
		Expression param1 = param.getLeafExpression();
		Object result1 = param1.calculate(ctx);
		if (result1 instanceof String) {
			if (option == null || option.indexOf('q') == -1) {
				return ((String)result1).toUpperCase();
			} else {
				return upper((String)result1);
			}
		} else if (result1 == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("upper" + mm.getMessage("function.paramTypeError"));
		}
	}

	// 忽略引号内的
	private String upper(String str) {
		int len = str.length();
		if (len < 3) {
			return str.toUpperCase();
		}
		
		String result = "";
		int i = 0;
		while (true) {
			int index = str.indexOf('"', i);
			if (index < 0) {
				index = str.indexOf('\'', i);
				if (index < 0) {
					if (i == 0) {
						return str.toUpperCase();
					} else {
						return result + str.substring(i).toUpperCase();
					}
				}
			}
			
			int match = Sentence.scanQuotation(str, index);
			if (match > 0) {
				result += str.substring(i, index).toUpperCase();
				result += str.substring(index, match + 1);
				i = match + 1;
			} else {
				if (i == 0) {
					return str.toUpperCase();
				} else {
					return result + str.substring(i).toUpperCase();
				}
			}
		}
	}
}