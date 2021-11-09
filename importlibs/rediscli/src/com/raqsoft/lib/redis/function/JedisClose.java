package com.raqsoft.lib.redis.function;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

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
