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
			
			int n = ((Number)obj).intValue();
			return srcStr.substring(n - 1, n);
		} else if (param.getType() == IParam.Colon) { // :
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
			}
			
			int start = 0;
			IParam sub0 = param.getSub(0);
			if (sub0 != null) {
				Object obj = sub0.getLeafExpression().calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
				}
				
				start = ((Number)obj).intValue() - 1;
			}
			
			IParam sub1 = param.getSub(1);
			if (sub1 != null) {
				Object obj = sub1.getLeafExpression().calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
				}
				
				int end  = ((Number)obj).intValue();
				return srcStr.substring(start, end);
			} else {
				return srcStr.substring(start);
			}
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
					
					int n = ((Number)obj).intValue();
					result += srcStr.substring(n - 1, n);
				} else {
					if (sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
					}
					
					int start = 0;
					IParam sub0 = sub.getSub(0);
					if (sub0 != null) {
						Object obj = sub0.getLeafExpression().calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
						}
						
						start = ((Number)obj).intValue() - 1;
					}
					
					IParam sub1 = sub.getSub(1);
					if (sub1 != null) {
						Object obj = sub1.getLeafExpression().calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
						}
						
						int end  = ((Number)obj).intValue();
						result += srcStr.substring(start, end);
					} else {
						result += srcStr.substring(start);
					}
				}
			}
			
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
		}
	}
}