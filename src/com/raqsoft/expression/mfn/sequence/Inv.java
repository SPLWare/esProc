package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 返回数列的逆数列
 * p.inv(k)
 * @author RunQian
 *
 */
public class Inv extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.inv(srcSequence.length());
		} else if (param.isLeaf()) {
			Object val = param.getLeafExpression().calculate(ctx);
			if (val instanceof Number) {
				return srcSequence.inv(((Number)val).intValue());
			} else if (val instanceof Sequence) {
				return srcSequence.inv((Sequence)val, option);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("inv" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("inv" + mm.getMessage("function.invalidParam"));
		}
	}
}
