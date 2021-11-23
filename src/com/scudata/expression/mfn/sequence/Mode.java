package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
