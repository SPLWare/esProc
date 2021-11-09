package com.raqsoft.expression.operator;

import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.Operator;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.util.Variant;

/**
 * ‘ÀÀ„∑˚£∫%=
 * »°”‡∏≥÷µ
 * @author RunQian
 *
 */
public class ModAssign extends Operator {
	public ModAssign() {
		priority = PRI_EVL;
	}

	public Object calculate(Context ctx) {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"%=\"" + mm.getMessage("operator.missingLeftOperation"));
		}

		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"%=\"" + mm.getMessage("operator.missingRightOperation"));
		}

		Object o1 = left.calculate(ctx);
		Object o2 = right.calculate(ctx);
		return left.assign(Variant.mod(o1, o2), ctx);
	}
}
