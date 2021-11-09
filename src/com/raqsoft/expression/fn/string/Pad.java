package com.raqsoft.expression.fn.string;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;

/**
 * pad(s,c,l) 在字符串s前连续拼接字符串c，直到第一个字符串的总长度为l为止。
 * @author runqian
 *
 */
public class Pad extends Function {
	public Object calculate(Context ctx) {
		if (param == null || param.isLeaf()) {
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

		Object o1 = sub1.getLeafExpression().calculate(ctx);
		if (!(o1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.paramTypeError"));
		}

		Object o2 = sub2.getLeafExpression().calculate(ctx);
		if (!(o2 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pad" + mm.getMessage("function.paramTypeError"));
		}

		Object o3 = sub3.getLeafExpression().calculate(ctx);
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
