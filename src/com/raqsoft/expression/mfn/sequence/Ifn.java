package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 返回序列第一个不为空的元素
 * A.ifn(), A.ifn(x)
 * @author RunQian
 *
 */
public class Ifn extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.ifn();
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.ifn(exp, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifn" + mm.getMessage("function.invalidParam"));
		}
	}
}
