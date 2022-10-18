package com.scudata.lib.mongo.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// format: mongodb://[username:password@]host[:port]/database[?options&rows&ssl=true],[[connectTimeout,socketTimeout,serverSelectionTimeout],keyStorey:keyPwd]
public class ImOpen extends ImFunction {
	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_DB;
	}

	public Node optimize(Context ctx) {
		return this;
	}

	protected Object doQuery(Object[] objs) {
		if (objs.length<1){
			throw  new RQException("ImOpen invalidParam");
		}
		if (!(objs[0] instanceof String)){
			throw new RQException("connect function.paramTypeError");
		}
		
		return new ImMongo(objs, m_ctx);
	}
}

