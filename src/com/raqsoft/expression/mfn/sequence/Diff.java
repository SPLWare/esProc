package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 计算序列成员的差列
 * A.diff() A.diff(x)，A是序列的序列
 * @author RunQian
 *
 */
public class Diff extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.diff(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).diff(option);
		}

		MessageManager mm = EngineMessage.get();
		throw new RQException("diff" + mm.getMessage("function.invalidParam"));
	}
}
