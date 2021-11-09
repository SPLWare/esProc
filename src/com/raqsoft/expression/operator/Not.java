package com.raqsoft.expression.operator;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Operator;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * ·ÇÔËËã·û£º!
 * @author RunQian
 *
 */
public class Not extends Operator {
	public Not() {
		priority = PRI_NOT;
	}

	public Object calculate(Context ctx) {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"!\"" + mm.getMessage("operator.missingRightOperation"));
		}

		return Boolean.valueOf(Variant.isFalse(right.calculate(ctx)));
	}
}
