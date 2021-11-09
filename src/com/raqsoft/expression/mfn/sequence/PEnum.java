package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;

/**
 * 取值在枚举分组中属于哪一组
 * E.penum(y)
 * @author RunQian
 *
 */
public class PEnum extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("penum" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			return srcSequence.penum(obj, option, ctx, cs);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("penum" + mm.getMessage("function.invalidParam"));
		}
	}
}
