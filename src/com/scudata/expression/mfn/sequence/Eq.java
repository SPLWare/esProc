package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 判断序列是否与指定序列互为置换列
 * A.eq(B)
 * @author RunQian
 *
 */
public class Eq extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("eq" + mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			boolean b = srcSequence.isPeq((Sequence)obj);
			return Boolean.valueOf(b);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("eq" + mm.getMessage("function.paramTypeError"));
		}
	}
}
