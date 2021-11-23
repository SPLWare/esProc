package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;

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
