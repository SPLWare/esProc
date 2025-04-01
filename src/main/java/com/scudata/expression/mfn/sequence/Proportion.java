package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 计算序列的占比
 * A.proportion() A.proportion(x)
 * @author RunQian
 *
 */
public class Proportion extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.proportion();
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).proportion();
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException("proportion" + mm.getMessage("function.invalidParam"));
	}
}
