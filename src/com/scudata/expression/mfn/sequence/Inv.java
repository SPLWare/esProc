package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

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
