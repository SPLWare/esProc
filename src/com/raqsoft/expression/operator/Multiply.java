package com.raqsoft.expression.operator;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Operator;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * ³Ë·¨ÔËËã·û£º*
 * @author RunQian
 *
 */
public class Multiply extends Operator {
	public Multiply() {
		priority = PRI_MUL;
	}

	public Object calculate(Context ctx) {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"*\"" + mm.getMessage("operator.missingRightOperation"));
		}

		return Variant.multiply(left.calculate(ctx), right.calculate(ctx));
	}
}
