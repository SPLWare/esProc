package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 计算序列成员的交列
 * A.isect() A.isect(x)，A是序列的序列
 * @author RunQian
 *
 */
public class Isect extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.isect(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).isect(option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("isect" + mm.getMessage("function.invalidParam"));
		}
	}
}
