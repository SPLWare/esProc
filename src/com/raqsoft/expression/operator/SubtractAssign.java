package com.raqsoft.expression.operator;

import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Operator;

/**
 * ÔËËã·û£º-=
 * @author RunQian
 *
 */
public class SubtractAssign extends Operator {
	public SubtractAssign() {
		priority = PRI_EVL;
	}

	public Object calculate(Context ctx) {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"-=\"" + mm.getMessage("operator.missingRightOperation"));
		}

		Object o1 = left.calculate(ctx);
		Object o2 = right.calculate(ctx);
		return left.assign(Variant.subtract(o1, o2), ctx);
	}
}
