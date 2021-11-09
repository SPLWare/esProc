package com.raqsoft.expression.operator;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Operator;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * ³ý·¨ÔËËã·û£º/
 * @author RunQian
 *
 */
public class Divide extends Operator {
	public Divide() {
		priority = PRI_DIV;
	}

	public Object calculate(Context ctx) {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"/\"" + mm.getMessage("operator.missingRightOperation"));
		}

		return Variant.divide(left.calculate(ctx), right.calculate(ctx));
	}
}
