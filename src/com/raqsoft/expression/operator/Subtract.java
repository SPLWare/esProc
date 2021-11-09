package com.raqsoft.expression.operator;

import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.Operator;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.util.Variant;

/**
 * ÔËËã·û£º-
 * @author RunQian
 *
 */
public class Subtract extends Operator {
	public Subtract() {
		priority = PRI_SUB;
	}

	public Object calculate(Context ctx) {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"-\"" + mm.getMessage("operator.missingRightOperation"));
		}

		return Variant.subtract(left.calculate(ctx), right.calculate(ctx));
	}
}
