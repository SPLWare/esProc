package com.raqsoft.expression.fn.string;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;

/**
 * È¥µô×Ö·û´®Á½¶ËµÄ¿Õ´®
 * @author runqian
 *
 */
public class Trim extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("trim" + mm.getMessage("function.invalidParam"));
		}

		Object result1 = param.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return result1;
		}
		if (! (result1 instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("trim" + mm.getMessage("function.paramTypeError"));
		}

		String str = (String) result1;
		if (option == null) {
			return str.trim();
		} else if (option.indexOf('l') != -1) {
			int i = 0, ilength = str.length();
			for (i = 0; i < ilength; i++) {
				if (!Character.isWhitespace(str.charAt(i))) {
					break;
				}
			}
			if (i >= str.length()) {
				return "";
			} else {
				return str.substring(i);
			}
		} else if (option.indexOf('r') != -1) {
			int i = 0;
			for (i = str.length() - 1; i >= 0; i--) {
				if (!Character.isWhitespace(str.charAt(i))) {
					break;
				}
			}
			if (i < 0) {
				return "";
			} else {
				return str.substring(0, i + 1);
			}
		} else if (option.indexOf('a') != -1) {
			char []chars = str.toCharArray();
			int len = chars.length;
			StringBuffer sb = new StringBuffer(len);
			boolean isPrevWhiteSpace = true;
			for (int i = 0; i < len; ++i) {
				char c = chars[i];
				if (Character.isWhitespace(c)) {
					if (!isPrevWhiteSpace) {
						isPrevWhiteSpace = true;
						sb.append(c);
					}
				} else if (c == '"' || c == '\'') {
					int match = scanString(chars, i);
					if (match > 0) {
						sb.append(chars, i, match - i + 1);
						i = match;
					} else {
						sb.append(c);
					}
					
					isPrevWhiteSpace = false;
				} else if (isSymbol(c)) {
					int last = sb.length() - 1;
					if (last >= 0 && Character.isWhitespace(sb.charAt(last))) {
						sb.deleteCharAt(last);
					}
					
					sb.append(c);
					isPrevWhiteSpace = true;
				} else {
					sb.append(c);
					isPrevWhiteSpace = false;
				}
			}
			
			int last = sb.length() - 1;
			if (last >= 0 && Character.isWhitespace(sb.charAt(last))) {
				sb.deleteCharAt(last);
			}
			
			return sb.toString();
		} else {
			return str.trim();
		}
	}
	
	private static int scanString(char []chars, int start) {
		char q = chars[start++];
		for (int len = chars.length; start < len; ++start) {
			if (chars[start] == q) {
				start++;
				if (start == len || chars[start] != q) {
					return start;
				}
			}
		}

		return -1;
	}
	
	private static boolean isSymbol(char c) {
		return c == '+' || c == '-' || c == '*' || c == '/' || c == '%' ||
			c == '=' || c == '&' || c == '|' || c == '!' ||
			c == ',' || c == '>' || c == '<' || c == '(' || c == ')' ||
			c == '[' || c == ']' || c == ':' || c == '{' || c == '}' ||
			c == '^' || c == '.';
	}
}
