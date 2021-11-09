package com.raqsoft.expression.fn.convert;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;

/**
 * ift(x) ÅÐ¶ÏxÊÇ·ñÎªÐò±í
 * @author runqian
 *
 */
public class IfTable extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ift" + mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		return (obj instanceof Table) ? Boolean.TRUE : Boolean.FALSE;
	}
}
