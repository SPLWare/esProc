package com.raqsoft.expression.fn.convert;

import com.raqsoft.common.Escape;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.common.StringUtils;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 将对象的数据类型转换成字符型
 * string(expression{, format}:loc) 
 * 将对象的数据类型转换成字符型，转换过程中可以格式化；loc为语言，仅对日期时间适用，缺省使用系统语言。
 * @author runqian
 *
 */
public class ToString extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("string"+mm.getMessage("function.missingParam"));
		}
		
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
}
