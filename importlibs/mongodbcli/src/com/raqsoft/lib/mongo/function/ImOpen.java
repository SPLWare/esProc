package com.raqsoft.lib.mongo.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class ImOpen extends Function {
	
	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_DB;
	}

	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("mongodb" + mm.getMessage("function.invalidParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		if (!(obj instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("connect" + mm.getMessage("function.paramTypeError"));
		}

		return new ImMongo((String)obj, ctx);
	}
}

