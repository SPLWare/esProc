package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 返回序列中取值为真（非空并且不是false）的元素的个数
 * A.count(), A.count(x)
 * @author RunQian
 *
 */
public class Count extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return new Integer(srcSequence.count(option));
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return new Integer(srcSequence.count(exp, option, ctx));
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("count" + mm.getMessage("function.invalidParam"));
		}
	}
}
