package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

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
