package com.scudata.lib.mongo.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

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

