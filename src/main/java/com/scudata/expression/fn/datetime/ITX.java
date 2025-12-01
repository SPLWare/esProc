package com.scudata.expression.fn.datetime;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * itx(s,d,m) 生成时间区间常数，s是秒，d是天，m是月
 * @author runqian
 *
 */
public class ITX extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("itx" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		int size = param.getSubSize();
		if (size == 0) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (result1 instanceof Number) {
				return new TimeInterval(((Number)result1).longValue());
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("itx" + mm.getMessage("function.paramTypeError"));
			}
		} else if (size == 2){
			IParam sub2 = param.getSub(1);
			if (sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("itx" + mm.getMessage("function.invalidParam"));
			}
			
			Object result1 = sub2.getLeafExpression().calculate(ctx);
			if (result1 instanceof Number) {
				return new TimeInterval(((Number)result1).intValue());
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("itx" + mm.getMessage("function.paramTypeError"));
			}
		} else if (size == 3) {
			IParam sub2 = param.getSub(1);
			IParam sub3 = param.getSub(2);
			if (sub2 == null|| sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("itx" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub2.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("itx" + mm.getMessage("function.paramTypeError"));
			}

			int day = ((Number)obj).intValue();
			obj = sub3.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("itx" + mm.getMessage("function.paramTypeError"));
			}

			int month = ((Number)obj).intValue();
			return new TimeInterval(month, day);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("itx" + mm.getMessage("function.invalidParam"));
		}
	}
}
