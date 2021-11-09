package com.raqsoft.lib.spark.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

// spark_open(hdfs, thrift, instanceName)
public class SparkOpen extends Function {
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_client" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		if (size != 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_client" + mm.getMessage("function.invalidParam"));
		}
		
		Object objs[] = new Object[size];
		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("spark_client" + mm.getMessage("function.invalidParam"));
			}
			objs[i] = param.getSub(i).getLeafExpression().calculate(ctx);
			if (!(objs[i] instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("spark_client" + mm.getMessage("function.paramTypeError"));
			}
		}
		
		return new SparkCli(ctx, (String)objs[0], (String)objs[1],(String)objs[2]);
	}
}
