package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * jvm(name) 取JVM属性
 * @author runqian
 *
 */
public class JVM extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("jvm" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("jvm" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof String) {
			String value = System.getProperty((String)obj);
			if (value != null) {
				return value;
			} else {
				return System.getenv((String)obj);
			}
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("jvm" + mm.getMessage("function.paramTypeError"));
		}
	}
}
