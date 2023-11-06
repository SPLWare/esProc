package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 计算序列的累积值
 * A.cumulate() A.cumulate(x)
 * @author RunQian
 *
 */
public class Cumulate extends SequenceFunction {

	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.cumulate();
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).cumulate();
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException("cumulate" + mm.getMessage("function.invalidParam"));
	}
}
