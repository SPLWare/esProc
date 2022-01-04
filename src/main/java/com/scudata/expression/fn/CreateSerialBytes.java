package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.SerialBytes;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 生成指定字节长度的排号。
 * k(n:l) k(ni,…)
 * @author runqian
 *
 */
public class CreateSerialBytes extends Function {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("k" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object n = param.getLeafExpression().calculate(ctx);
			if (n instanceof Number) {
				return new SerialBytes((Number)n, 1);
			} else if (n == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("k" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getType() == IParam.Colon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("k" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("k" + mm.getMessage("function.invalidParam"));
			}
			
			Object len = sub1.getLeafExpression().calculate(ctx);
			if (!(len instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("k" + mm.getMessage("function.paramTypeError"));
			}
			
			Object n = sub0.getLeafExpression().calculate(ctx);
			if (n instanceof Number) {
				return new SerialBytes((Number)n, ((Number)len).intValue());
			} else if (n == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("k" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			int len = param.getSubSize();
			Number []numbers = new Number[len];
			int []lens = new int[len];
			for (int i = 0; i < len; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("k" + mm.getMessage("function.invalidParam"));
				} else if (sub.isLeaf()) {
					Object n = sub.getLeafExpression().calculate(ctx);
					if (!(n instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("k" + mm.getMessage("function.paramTypeError"));
					}
					
					numbers[i] = (Number)n;
					lens[i] = 1;
				} else if (sub.getSubSize() == 2) {
					IParam sub0 = sub.getSub(0);
					IParam sub1 = sub.getSub(1);
					if (sub0 == null || sub1 == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("k" + mm.getMessage("function.invalidParam"));
					}
					
					Object obj0 = sub0.getLeafExpression().calculate(ctx);
					if (!(obj0 instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("k" + mm.getMessage("function.paramTypeError"));
					}
					
					Object obj1 = sub1.getLeafExpression().calculate(ctx);
					if (!(obj1 instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("k" + mm.getMessage("function.paramTypeError"));
					}
					
					numbers[i] = (Number)obj0;
					lens[i] = ((Number)obj1).intValue();
				} else {
					MessageManager mm = EngineMessage.get();
					throw new RQException("k" + mm.getMessage("function.invalidParam"));
				}
			}
			
			return new SerialBytes(numbers, lens);
		}
	}
}
