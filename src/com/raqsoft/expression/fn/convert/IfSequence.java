package com.raqsoft.expression.fn.convert;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.resources.EngineMessage;

/**
 * ifa(x) ÅÐ¶ÏxÊÇ·ñÎªÐòÁÐ
 * @author runqian
 *
 */
public class IfSequence extends Function {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifa" + mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		return (obj instanceof Sequence) ? Boolean.TRUE : Boolean.FALSE;
	}
}
