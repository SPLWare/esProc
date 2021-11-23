package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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