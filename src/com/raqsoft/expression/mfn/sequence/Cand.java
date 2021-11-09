package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 针对序列的成员做逻辑与运算
 * A.cand()、A.cand(x)
 * @author RunQian
 *
 */
public class Cand extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.cand();
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.cand(exp, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cand" + mm.getMessage("function.invalidParam"));
		}
	}
}