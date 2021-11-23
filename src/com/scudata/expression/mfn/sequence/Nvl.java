package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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