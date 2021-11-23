package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
