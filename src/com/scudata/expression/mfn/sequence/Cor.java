package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
