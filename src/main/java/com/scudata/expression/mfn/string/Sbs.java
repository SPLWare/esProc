package com.scudata.expression.mfn.string;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.IParam;
import com.scudata.expression.StringFunction;
import com.scudata.resources.EngineMessage;

/**
 * 取字符串指定位置的字符组成新字符串
 * s.sbs(a:b,…)
 * @author RunQian
 *
 */
public class Sbs extends StringFunction {
	private static String substring(String srcStr, int n) {
		int len = srcStr.length();
		if (n > len) {
			return "";
		} else if (n > 0) {
			return srcStr.substring(n - 1, n);
		} else if (n < 0) {
			n += len;
			if (n < 0) {
				return "";
			} else {
				return srcStr.substring(n, n + 1);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
		}
	}
	
	private static String substring(String srcStr, IParam startParam, IParam endParam, Context ctx) {
		int len = srcStr.length();
		int start = 0;
		if (startParam != null) {
			Object obj = startParam.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
			}
			
			start = ((Number)obj).intValue();
			if (start > len) {
				return "";
			} else if (start < 0) {
				start += len;
				if (start < 0) {
					start = 0;
				}
			} else if (start == 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
			} else {
				start--;
			}
		}
		
		if (endParam != null) {
			Object obj = endParam.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
			}
			
			int end  = ((Number)obj).intValue();
			if (end > len) {
				return srcStr.substring(start);
			} else if (end < 0) {
				end += len + 1;
			} else if (end == 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
			}
			
			if (start < end) {
				return srcStr.substring(start, end);
			} else {
				return "";
			}
		} else {
			return srcStr.substring(start);
		}
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sbs" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
			}
			
			return substring(srcStr, ((Number)obj).intValue());
		} else if (param.getType() == IParam.Colon) { // :
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
			}
			
			return substring(srcStr, param.getSub(0), param.getSub(1), ctx);
		} else if (param.getType() == IParam.Comma) { // ,
			String result = "";
			for (int i = 0, size = param.getSubSize(); i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
				} else if (sub.isLeaf()) {
					Object obj = sub.getLeafExpression().calculate(ctx);
					if (!(obj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
					}
					
					result += substring(srcStr, ((Number)obj).intValue());
				} else {
					if (sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
					}
					
					result += substring(srcStr, sub.getSub(0), sub.getSub(1), ctx);
				}
			}
			
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
		}
	}
}