package com.raqsoft.expression.operator;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Operator;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

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
