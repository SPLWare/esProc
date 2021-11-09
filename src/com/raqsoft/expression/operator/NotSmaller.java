package com.raqsoft.expression.operator;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Operator;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * ÔËËã·û£º>=
 * @author RunQian
 *
 */
public class NotSmaller extends Operator {
	public NotSmaller() {
		priority = PRI_NSL;
	}

	public Object calculate(Context ctx) {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\">=\"" + mm.getMessage("operator.missingLeftOperation"));
		}
		
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\">=\"" + mm.getMessage("operator.missingRightOperation"));
		}

		if (Variant.compare(left.calculate(ctx), right.calculate(ctx), true) >= 0) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
}
