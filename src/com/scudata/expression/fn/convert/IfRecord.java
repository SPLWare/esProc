package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Record;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * ifr(x) ÅÐ¶ÏxÊÇ·ñÎª¼ÇÂ¼
 * @author runqian
 *
 */
public class IfRecord extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifr" + mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		return (obj instanceof Record) ? Boolean.TRUE : Boolean.FALSE;
	}
}
