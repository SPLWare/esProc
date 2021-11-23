package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * sleep(n) ½«¼¯ËãÆ÷´¦ÓÚË¯Ãß×´Ì¬nºÁÃë
 * @author runqian
 *
 */
public class Sleep extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sleep" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			if (o instanceof Number) {
				try {
					Thread.sleep(((Number)o).longValue());
					return o;
				} catch (InterruptedException e) {
					throw new RQException(e.getMessage(), e);
				}
			} else if (o == null) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("sleep" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sleep" + mm.getMessage("function.invalidParam"));
		}
	}
}
