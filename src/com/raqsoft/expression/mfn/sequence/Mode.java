package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取序列中出现次数最多的成员
 * A.mode()
 * @author RunQian
 *
 */
public class Mode extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.mode();
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).mode();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mode" + mm.getMessage("function.invalidParam"));
		}
	}
}
