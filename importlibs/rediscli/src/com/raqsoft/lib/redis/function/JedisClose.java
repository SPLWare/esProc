package com.scudata.lib.redis.function;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// redis_close(handle)
public class JedisClose extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("redis_close" + mm.getMessage("function.missingParam"));
		}
		
		Object o = ctx.getParam("classLoader").getValue();
		if (o!=null && o instanceof ClassLoader){
			Thread.currentThread().setContextClassLoader((ClassLoader)o);
			ctx.setParamValue("classLoader", null);
		}

		Object handle = param.getLeafExpression().calculate(ctx);
		if (!(handle instanceof RedisTool)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("redis_close" + mm.getMessage("function.paramTypeError"));
		}
		ClassPathXmlApplicationContext appCtx=(ClassPathXmlApplicationContext) ((RedisTool)handle).getApplicationContext();
		appCtx.close();
		appCtx = null;
		return null;
	}
}
