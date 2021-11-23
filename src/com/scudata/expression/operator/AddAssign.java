package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;

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
