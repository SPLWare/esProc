package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * ÔËËã·û£º*=
 * ³ËµÈÓÚ
 * @author RunQian
 *
 */
public class MultiplyAssign extends Operator {
	public MultiplyAssign() {
		priority = PRI_EVL;
	}

	public Object calculate(Context ctx) {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"*=\"" + mm.getMessage("operator.missingRightOperation"));
		}

		Object o1 = left.calculate(ctx);
		Object o2 = right.calculate(ctx);

		return left.assign(Variant.multiply(o1, o2), ctx);
	}
}
