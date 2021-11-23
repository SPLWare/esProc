package com.scudata.lib.spark.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

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
