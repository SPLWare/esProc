package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * jvm(name) »°JVM Ù–‘
 * @author runqian
 *
 */
public class JVM extends Function {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("jvm" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
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
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("jvm" + mm.getMessage("function.invalidParam"));
		}
	}
}
