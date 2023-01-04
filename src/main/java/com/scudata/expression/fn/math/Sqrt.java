package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

public class Sqrt extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sqrt" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			Expression param1 = param.getLeafExpression();
			Object result = param1.calculate(ctx);
			if (result instanceof Number) {
				return new Double(Math.sqrt(((Number)result).doubleValue()));
			} else if (result == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sqrt" + mm.getMessage("function.invalidParam"));
			}
			
			Object a = sub0.getLeafExpression().calculate(ctx);
			if (a == null) {
				return null;
			} else if (!(a instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
			}
			
			Object b = sub1.getLeafExpression().calculate(ctx);
			if (!(b instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sqrt" + mm.getMessage("function.paramTypeError"));
			}
			
			double p = 1 / ((Number)b).doubleValue();
			return new Double(Math.pow(((Number)a).doubleValue(), p));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sqrt" + mm.getMessage("function.invalidParam"));
		}
	}

}
