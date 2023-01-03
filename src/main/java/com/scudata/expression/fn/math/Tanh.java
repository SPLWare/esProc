package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 返回某一数字的双曲正切值tanh(z)=(e^z-e^(-z))/(e^z+e^(-z))
 * @author yanjing
 *
 */
public class Tanh	extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("tanh" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("tanh" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Number) {
			return new Double(Math.tanh(((Number)obj).doubleValue()));
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("tanh" + mm.getMessage("function.paramTypeError"));
		}
	}
}
