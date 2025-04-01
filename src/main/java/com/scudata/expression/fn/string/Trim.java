package com.scudata.expression.fn.string;

import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.array.StringArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 去掉字符串两端的空串
 * @author runqian
 *
 */
public class Trim extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("trim" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("trim" + mm.getMessage("function.invalidParam"));
		}
	}
	
	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof String) {
			return trim((String)obj, option);
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("trim" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		IArray array = param.getLeafExpression().calculateAll(ctx);
		int size = array.size();
		
		if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			
			for (int i = 1; i <= size; ++i) {
				String str = stringArray.getString(i);
				if (str != null) {
					str = trim(str, option);
				} 
				
				result.push(str);
			}
			
			return result;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof String) {
				obj = trim((String)obj, option);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("trim" + mm.getMessage("function.paramTypeError"));
			}
			
			return new ConstArray(obj, size);
		} else {
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			
			for (int i = 1; i <= size; ++i) {
				Object obj = array.get(i);
				String str = null;
				if (obj instanceof String) {
					str = trim((String)obj, option);
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("trim" + mm.getMessage("function.paramTypeError"));
				}
				
				result.push(str);
			}
			
			return result;
		}
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		boolean[] signDatas;
		if (sign) {
			signDatas = signArray.isTrue().getDatas();
		} else {
			signDatas = signArray.isFalse().getDatas();
		}
		
		IArray array = param.getLeafExpression().calculateAll(ctx);
		int size = array.size();
		
		if (array instanceof StringArray) {
			StringArray stringArray = (StringArray)array;
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			
			for (int i = 1; i <= size; ++i) {
				if (signDatas[i] == false) {
					result.pushNull();
					continue;
				}
				
				String str = stringArray.getString(i);
				if (str != null) {
					str = trim(str, option);
				} 
				
				result.push(str);
			}
			
			return result;
		} else if (array instanceof ConstArray) {
			Object obj = array.get(1);
			if (obj instanceof String) {
				obj = trim((String)obj, option);
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("trim" + mm.getMessage("function.paramTypeError"));
			}
			
			return new ConstArray(obj, size);
		} else {
			StringArray result = new StringArray(size);
			result.setTemporary(true);
			
			for (int i = 1; i <= size; ++i) {
				if (signDatas[i] == false) {
					result.pushNull();
					continue;
				}
				
				Object obj = array.get(i);
				String str = null;
				if (obj instanceof String) {
					str = trim((String)obj, option);
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("trim" + mm.getMessage("function.paramTypeError"));
				}
				
				result.push(str);
			}
			
			return result;
		}
	}

	private static String trim(String str, String option) {
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
