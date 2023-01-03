package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * 返回某一数字的反双曲正弦值asinh(z)=ln(z+sqrt(z*z+1))
 * @author yanjing
 *
 */
public class Arcsinh extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("asinh" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("asinh" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Number) {
			double z = ((Number)obj).doubleValue();
			return new Double(Math.log(z+Math.sqrt(z*z+1)));
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("asinh" + mm.getMessage("function.paramTypeError"));
		}
	}
}
