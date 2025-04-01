package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
			return srcSequence.calc(exp, "o", ctx).icount(option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("icount" + mm.getMessage("function.invalidParam"));
		}
	}
}
