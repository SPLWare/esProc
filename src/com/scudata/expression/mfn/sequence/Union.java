package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 计算序列成员的并列
 * A.union() A.union(x)，A是序列的序列
 * @author RunQian
 *
 */
public class Union extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.union(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).union(option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("union" + mm.getMessage("function.invalidParam"));
		}
	}
}