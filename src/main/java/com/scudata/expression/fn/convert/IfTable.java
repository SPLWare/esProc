package com.scudata.expression.fn.convert;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

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
