package com.scudata.expression.fn.string;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * pad(s,c,l) 在字符串s前连续拼接字符串c，直到第一个字符串的总长度为l为止。
 * @author runqian
 *
 */
public class Pad extends Function {
	private Expression exp1;
	private Expression exp2;
	private Expression exp3;

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		IParam sub3 = param.getSub(2);
		if (sub1 == null || sub2 == null || sub3 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.invalidParam"));
		}
		
		exp1 = sub1.getLeafExpression();
		exp2 = sub2.getLeafExpression();
		exp3 = sub3.getLeafExpression();
	}

	public Object calculate(Context ctx) {
		Object o1 = exp1.calculate(ctx);
		if (!(o1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.paramTypeError"));
		}

		Object o2 = exp2.calculate(ctx);
		if (!(o2 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.paramTypeError"));
		}

		Object o3 = exp3.calculate(ctx);
		if (!(o3 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.paramTypeError"));
		}

		return pad((String)o1, (String)o2, ((Number)o3).intValue(), option);
	}

	private static String pad(String src, String pad, int len, String opt) {
		int srcLen = src.length();
		if (len <= srcLen) return src;

		int padLen = pad.length();
		if (padLen < 1) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.invalidParam"));
		}

		char []chars = new char[len];
		int start;
		int end;
		if (opt == null || opt.indexOf('r') == -1) {
			start = 0;
			end = len - srcLen;
			System.arraycopy(src.toCharArray(), 0, chars, end, srcLen);
		} else {
			start = srcLen;
			end = len;
			System.arraycopy(src.toCharArray(), 0, chars, 0, srcLen);
		}

		if (padLen == 1) {
			char c = pad.charAt(0);
			for (; start < end; ++start) {
				chars[start] = c;
			}

			return new String(chars);
		} else {
			char []pads = pad.toCharArray();
			while (true) {
				for (int i = 0; i < padLen; ++i) {
					chars[start++] = pads[i];
					if (start == end) return new String(chars);
				}
			}
		}
	}
}
