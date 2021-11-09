package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取序列的每个元素的排名返回成新序列
 * A.ranks(), A.ranks(x)
 * @author RunQian
 *
 */
public class Ranks extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.ranks(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).ranks(option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ranks" + mm.getMessage("function.invalidParam"));
		}
	}
}
