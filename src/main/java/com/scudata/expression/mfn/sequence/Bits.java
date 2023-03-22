package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 把位值序列转成数序列
 * A.bits(), A.bits(n)
 * @author RunQian
 *
 */
public class Bits extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.bits(option);
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof Number)) {
				
			}
			
			return srcSequence.bits(((Number)obj).intValue(), option);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sum" + mm.getMessage("function.invalidParam"));
		}
	}
}
