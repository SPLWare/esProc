package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
			return srcSequence.calc(exp, "o", ctx).ranks(option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ranks" + mm.getMessage("function.invalidParam"));
		}
	}
}
