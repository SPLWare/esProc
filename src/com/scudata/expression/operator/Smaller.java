package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * ÔËËã·û£º<
 * @author RunQian
 *
 */
public class Smaller extends Operator {
	public Smaller() {
		priority = PRI_SL;
	}

	public Object calculate(Context ctx) {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"<\"" + mm.getMessage("operator.missingLeftOperation"));
		}
		
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"<\"" + mm.getMessage("operator.missingRightOperation"));
		}

		if (Variant.compare(left.calculate(ctx), right.calculate(ctx), true) < 0) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}
}
