package com.scudata.expression.fn.convert;

import java.util.Date;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 将字符串、数字或日期转换成64位长整数
 * @author runqian
 *
 */
public class ToLong extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("long" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object result = param.getLeafExpression().calculate(ctx);
			if (result instanceof Long) {
				return result;
			} else if (result instanceof Number) {
				return new Long(((Number)result).longValue());
			} else if (result instanceof String) {
				try {
					String str = (String)result;
					if (str.length() > 2 && str.charAt(0) == '0' && (str.charAt(1) == 'X' || str.charAt(1) == 'x')) {
						return Long.parseLong(str, 16);
					} else {
						return Long.parseLong(str);
					}
				} catch (NumberFormatException e) {
					return null;
				}
			} else if (result instanceof Date) {
				return new Long(((Date)result).getTime());
			} else if (result == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("long" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("long" + mm.getMessage("function.invalidParam"));
			}
			
			Object str = sub0.getLeafExpression().calculate(ctx);
			if (!(str instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("long" + mm.getMessage("function.paramTypeError"));
			}
			
			Object radix = sub1.getLeafExpression().calculate(ctx);
			if (!(radix instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("long" + mm.getMessage("function.paramTypeError"));
			}
			
			try {
				return Long.parseLong((String)str, ((Number)radix).intValue());
			} catch (NumberFormatException e) {
				return null;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("long" + mm.getMessage("function.invalidParam"));
		}
	}
}
