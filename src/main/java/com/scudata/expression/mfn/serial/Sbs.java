package com.scudata.expression.mfn.serial;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.IParam;
import com.scudata.expression.SerialFunction;
import com.scudata.resources.EngineMessage;

/**
 * 取排号的长度
 * k.sbs(a:b,…)
 * @author RunQian
 *
 */
public class Sbs extends SerialFunction {
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
			return sb.getByte(n);
		} else if (param.getType() == IParam.Colon) { // :
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
			}
			
			int start = 1;
			IParam sub0 = param.getSub(0);
			if (sub0 != null) {
				Object obj = sub0.getLeafExpression().calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
				}
				
				start = ((Number)obj).intValue();
			}
			
			int end;
			IParam sub1 = param.getSub(1);
			if (sub1 != null) {
				Object obj = sub1.getLeafExpression().calculate(ctx);
				if (!(obj instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
				}
				
				end  = ((Number)obj).intValue();
			} else {
				end = sb.length();
			}

			return sb.getBytes(start, end);
		} else if (param.getType() == IParam.Comma) { // ,
			long result = 0;
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
					result = (result << 8) + sb.getByte(n);
				} else {
					if (sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
					}
					
					int start = 1;
					IParam sub0 = sub.getSub(0);
					if (sub0 != null) {
						Object obj = sub0.getLeafExpression().calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
						}
						
						start = ((Number)obj).intValue();
					}
					
					int end;
					IParam sub1 = sub.getSub(1);
					if (sub1 != null) {
						Object obj = sub1.getLeafExpression().calculate(ctx);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("sbs" + mm.getMessage("function.paramTypeError"));
						}
						
						end  = ((Number)obj).intValue();
					} else {
						end = sb.length();
					}
					
					int bytes = 8 * (end - start + 1);
					result = (result << bytes) + sb.getBytes(start, end);
				}
			}
			
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sbs" + mm.getMessage("function.invalidParam"));
		}
	}
}
