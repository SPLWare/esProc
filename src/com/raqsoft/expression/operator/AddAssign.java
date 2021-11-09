package com.raqsoft.expression.operator;

import com.raqsoft.resources.EngineMessage;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Operator;

/**
 * ÔËËã·û£º+=
 * @author RunQian
 *
 */
public class AddAssign extends Operator {
	public AddAssign() {
		priority = PRI_EVL;
	}

	public Object calculate(Context ctx) {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"+=\"" + mm.getMessage("operator.missingRightOperation"));
		}

		return left.addAssign(right.calculate(ctx), ctx);
	}
}
