package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * ifstring(x) 判定参数x是否为字符串类型
 * @author runqian
 *
 */
public class IfString extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifstring" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifstring" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		return (obj instanceof String) ? Boolean.TRUE : Boolean.FALSE;
	}
}
