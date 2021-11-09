package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取序列中第一个非空并且不是空串的元素
 * A.nvl()
 * @author RunQian
 *
 */
public class Nvl extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.nvl();
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.nvl(exp, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("nvl" + mm.getMessage("function.invalidParam"));
		}
	}
}