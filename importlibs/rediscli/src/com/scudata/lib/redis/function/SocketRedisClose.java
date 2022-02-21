package com.scudata.lib.redis.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// redis_close(handle)
public class SocketRedisClose extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("redis_close" + mm.getMessage("function.missingParam"));
		}
		
		return "success";
	}
}
