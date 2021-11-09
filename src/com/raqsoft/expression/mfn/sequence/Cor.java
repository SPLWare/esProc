package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 针对序列的成员做逻辑或运算
 * A.cor()、A.cor(x)
 * @author RunQian
 *
 */
public class Cor extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.cor();
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.cor(exp, ctx);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("cor" + mm.getMessage("function.invalidParam"));
		}
	}
}
