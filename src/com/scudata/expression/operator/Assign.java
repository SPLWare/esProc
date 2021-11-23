package com.scudata.expression.operator;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Operator;
import com.scudata.resources.EngineMessage;

/**
 * ÔËËã·û£º=
 * @author RunQian
 *
 */
public class Assign extends Operator {
	public Assign() {
		priority = PRI_EVL;
	}

	public Object calculate(Context ctx) {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"=\"" + mm.getMessage("operator.missingLeftOperation"));
		}
		
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"=\"" +	mm.getMessage("operator.missingRightOperation"));
		}

		return left.assign(right.calculate(ctx), ctx);
	}

	// a = b = c =1
	public Object assign(Object value, Context ctx) {
		if (left == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"=\"" + mm.getMessage("operator.missingLeftOperation"));
		}
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"=\"" +	mm.getMessage("operator.missingRightOperation"));
		}

		return left.assign(right.assign(value, ctx), ctx);
	}

	public byte calcExpValueType(Context ctx) {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"=\"" +	mm.getMessage("operator.missingRightOperation"));
		}
		return right.calcExpValueType(ctx);
	}
}
