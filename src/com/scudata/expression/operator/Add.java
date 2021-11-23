package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

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
