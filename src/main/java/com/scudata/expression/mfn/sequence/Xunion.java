package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
			return srcSequence.calc(exp, "o", ctx).xor();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("xunion" + mm.getMessage("function.invalidParam"));
		}
	}
}
