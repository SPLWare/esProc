package com.scudata.expression.fn.convert;

import com.scudata.array.IArray;
import com.scudata.array.StringArray;
import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 将对象的数据类型转换成字符型
 * string(expression{, format}:loc) 
 * 将对象的数据类型转换成字符型，转换过程中可以格式化；loc为语言，仅对日期时间适用，缺省使用系统语言。
 * @author runqian
 *
 */
public class ToString extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("string" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object val;
		String fmt = null;
		String locale = null;
		
		if (param.isLeaf()) {
			val = param.getLeafExpression().calculate(ctx);
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("string" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("string" + mm.getMessage("function.invalidParam"));
			}
			
			val = sub0.getLeafExpression().calculate(ctx);
			if (sub1.isLeaf()) {
				Object obj = sub1.getLeafExpression().calculate(ctx);
				if (obj instanceof String) {
					fmt = (String)obj;
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("string" + mm.getMessage("function.paramTypeError"));
				}
			} else {
				IParam fmtParam = sub1.getSub(0);
				IParam locParam = sub1.getSub(1);
				if (fmtParam == null || locParam == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("string" + mm.getMessage("function.invalidParam"));
				}

				Object obj = fmtParam.getLeafExpression().calculate(ctx);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("string" + mm.getMessage("function.paramTypeError"));
				}
				
				fmt = (String)obj;
				obj = locParam.getLeafExpression().calculate(ctx);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("string" + mm.getMessage("function.paramTypeError"));
				}
				
				locale = (String)obj;
			}
		}
		
		if (val instanceof String) {
			String str = (String)val;
			String opt = this.option;
			if (opt != null) {
				if (opt.indexOf('u') != -1) {
					StringBuffer sb = new StringBuffer(str.length() + 16);
					StringUtils.deunicode(str, sb, "\"'");
					str = sb.toString();
				} else if (opt.indexOf('e') != -1) {
					str = Escape.add(str);
				}
				
				if (opt.indexOf('q') != -1) {
					str = '"' + str + '"';
				}
			}
			
			return str;
		} else {
			if (fmt == null) {
				return Variant.toString(val);
			} else if (locale == null) {
				return Variant.format(val, fmt);
			} else {
				return Variant.format(val, fmt, locale);
			}
		}
	}

	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (param.isLeaf()) {
			IArray array = param.getLeafExpression().calculateAll(ctx);
			boolean isUnicode = false, isEscape = false, isQuote = false;
			if (option != null) {
				if (option.indexOf('u') != -1) {
					isUnicode = true;
				} else if (option.indexOf('e') != -1) {
					isEscape = true;
				}
				
				if (option.indexOf('q') != -1) {
					isQuote = true;
				}
			}
			
			int len = array.size();
			StringArray result = new StringArray(len);
			result.setTemporary(true);
			
			if (isUnicode) {
				StringBuffer sb = new StringBuffer(128);
				for (int i = 1; i <= len; ++i) {
					Object val = array.get(i);
					String str;
					if (val instanceof String) {
						StringUtils.deunicode((String)val, sb, "\"'");
						str = sb.toString();
						sb.setLength(0);
					} else {
						str = Variant.toString(val);
					}
					
					if (isQuote) {
						result.pushString('"' + str + '"');
					} else {
						result.pushString(str);
					}
				}
			} else if (isEscape) {
				for (int i = 1; i <= len; ++i) {
					Object val = array.get(i);
					String str;
					if (val instanceof String) {
						str = Escape.add((String)val);
					} else {
						str = Variant.toString(val);
					}
					
					if (isQuote) {
						result.pushString('"' + str + '"');
					} else {
						result.pushString(str);
					}
				}
			} else {
				for (int i = 1; i <= len; ++i) {
					Object val = array.get(i);
					String str;
					if (val instanceof String) {
						str = (String)val;
					} else {
						str = Variant.toString(val);
					}
					
					if (isQuote) {
						result.pushString('"' + str + '"');
					} else {
						result.pushString(str);
					}
				}
			}
			
			return result;
		}
		
		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("string" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		if (sub0 == null || sub1 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("string" + mm.getMessage("function.invalidParam"));
		}
		
		String fmt = null;
		String locale = null;
		if (sub1.isLeaf()) {
			Object obj = sub1.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				fmt = (String)obj;
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("string" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			IParam fmtParam = sub1.getSub(0);
			IParam locParam = sub1.getSub(1);
			if (fmtParam == null || locParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("string" + mm.getMessage("function.invalidParam"));
			}

			Object obj = fmtParam.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("string" + mm.getMessage("function.paramTypeError"));
			}
			
			fmt = (String)obj;
			obj = locParam.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("string" + mm.getMessage("function.paramTypeError"));
			}
			
			locale = (String)obj;
		}
		
		IArray array = sub0.getLeafExpression().calculateAll(ctx);
		int len = array.size();
		StringArray result = new StringArray(len);
		result.setTemporary(true);
		
		if (locale == null) {
			for (int i = 1; i <= len; ++i) {
				String str = Variant.format(array.get(i), fmt);
				result.pushString(str);
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				String str = Variant.format(array.get(i), fmt, locale);
				result.pushString(str);
			}
		}
		
		return result;
	}
}
