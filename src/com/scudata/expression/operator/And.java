package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * ÔËËã·û£º&&
 * @author RunQian
 *
 */
public class And extends Operator {
	public And() {
		priority = PRI_AND;
	}

	public Object calculate(Context ctx) {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"&&\"" + mm.getMessage("operator.missingLeftOperation"));
		}
		
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"&&\"" + mm.getMessage("operator.missingRightOperation"));
		}

		Object value = left.calculate(ctx);
		if (Variant.isTrue(value)) {
			value = right.calculate(ctx);
			if (value instanceof Boolean) {
				return value;
			} else {
				return Boolean.valueOf(value != null);
			}
		} else {
			return Boolean.FALSE;
		}
	}
}
