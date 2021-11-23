package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

public class Round extends Function {

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null) {
			throw new RQException("round" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object o = param.getLeafExpression().calculate(ctx);
			return Variant.round(o);
		} else {
			if (param.getSubSize() != 2) {
				throw new RQException("round" + mm.getMessage("function.invalidParam"));
			}

			IParam p1 = param.getSub(0);
			IParam p2 = param.getSub(1);
			if (p1 == null || p2 == null) {
				throw new RQException("round" + mm.getMessage("function.invalidParam"));
			}

			Object o1 = p1.getLeafExpression().calculate(ctx);
			Object o2 = p2.getLeafExpression().calculate(ctx);
			if (o2 instanceof Number) {
				return Variant.round(o1, ((Number)o2).intValue());
			} else if (o2 == null) {
				return Variant.round(o1);
			} else {
				throw new RQException("round" + mm.getMessage("function.paramTypeError"));
			}
		}
	}
}
