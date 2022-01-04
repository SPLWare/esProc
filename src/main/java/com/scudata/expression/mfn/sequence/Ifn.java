package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
