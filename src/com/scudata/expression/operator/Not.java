package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

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
