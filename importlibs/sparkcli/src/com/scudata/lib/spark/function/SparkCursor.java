package com.scudata.lib.spark.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.resources.EngineMessage;

public class SparkCursor extends ImFunction {
	public Object doQuery(Object[] objs) {
		if (objs == null || objs.length < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_query1" + mm.getMessage(Integer.toString(param.getSubSize())));
		}
	
		if (!(objs[0] instanceof SparkCli)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_query4" + mm.getMessage("function.paramTypeError"));
		}
		
		SparkCli client = (SparkCli)objs[0];
		if (!(objs[1] instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_query5" + mm.getMessage("function.paramTypeError"));
		}
		
		return  client.cursorQuery((String)objs[1], m_ctx);
	}
}
