package com.scudata.lib.redis.function;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class SocketRedisOpen extends Function {
	
	protected nl.melp.redis.Redis redis = null;
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		try {
			Object objs[] = new Object[1];	
			if (param != null) {
				if (param.isLeaf()){			
					objs[0] = param.getLeafExpression().calculate(ctx);
					if (!(objs[0] instanceof String)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("redis_open" + mm.getMessage("function.paramTypeError"));
					}			
				}
			}
			
			String[] ss = objs[0].toString().split(":");
			String host = ss[0];
			int port = 6379;
			if (ss.length>1) port = Integer.parseInt(ss[1]);
			redis = new nl.melp.redis.Redis(new Socket(host, port));
		} catch (Exception e) {
			return null;
		}

		return this;
	}
	
}
