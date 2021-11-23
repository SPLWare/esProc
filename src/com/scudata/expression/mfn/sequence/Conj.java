package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 计算序列成员的和列
 * A.conj() A.conj(x)，A是序列的序列
 * @author RunQian
 *
 */
public class Conj extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.conj(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).conj(option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("conj" + mm.getMessage("function.invalidParam"));
		}
	}
}