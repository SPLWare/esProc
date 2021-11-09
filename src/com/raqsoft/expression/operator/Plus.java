package com.raqsoft.expression.operator;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Operator;
import com.raqsoft.resources.EngineMessage;

/**
 * ÕýºÅÔËËã·û£º+
 * @author RunQian
 *
 */
public class Plus extends Operator {
	public Plus() {
		this.priority = PRI_PLUS;
	}

	public Object calculate(Context ctx) {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"+\"" + mm.getMessage("operator.missingRightOperation"));
		}

		Object rightResult = right.calculate(ctx);
		if (rightResult instanceof Number || rightResult == null) {
			return rightResult;
		}
		
		MessageManager mm = EngineMessage.get();
		throw new RQException("\"+\"" + mm.getMessage("operator.numberRightOperation"));
	}
}
