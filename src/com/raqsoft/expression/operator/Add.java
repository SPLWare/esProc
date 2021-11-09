package com.raqsoft.expression.operator;

import com.raqsoft.resources.EngineMessage;
import com.raqsoft.expression.Operator;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.util.Variant;

/**
 * ÔËËã·û£º+
 * @author RunQian
 *
 */
public class Add extends Operator {
	public Add() {
		priority = PRI_ADD;
	}
	
	public Object calculate(Context ctx) {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"+\"" + mm.getMessage("operator.missingRightOperation"));
		}

		return Variant.add(left.calculate(ctx), right.calculate(ctx));
	}
}
