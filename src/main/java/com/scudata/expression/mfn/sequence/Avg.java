package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 计算序列平均值
 * A.avg(), A.avg(x)
 * @author RunQian
 *
 */
public class Avg extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.average();
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, "o", ctx).average();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("avg" + mm.getMessage("function.invalidParam"));
		}
	}
}
