package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 返回序列中取值为真（非空并且不是false）的非重复元素数量
 * A.icount()
 * @author RunQian
 *
 */
public class ICount extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.icount(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).icount(option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("icount" + mm.getMessage("function.invalidParam"));
		}
	}
}
