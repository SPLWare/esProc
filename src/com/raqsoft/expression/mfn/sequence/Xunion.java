package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 计算序列成员的异或列
 * A.xunion() A.xunion(x)，A是序列的序列
 * @author RunQian
 *
 */
public class Xunion extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.xor();
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.calc(exp, ctx).xor();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xunion" + mm.getMessage("function.invalidParam"));
		}
	}
}
