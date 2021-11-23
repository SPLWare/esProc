package com.scudata.lib.spark.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// spark_close(spark_client)
public class SparkClose extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_close" + mm.getMessage("function.missingParam"));
		}

		Object client = param.getLeafExpression().calculate(ctx);
		if (!(client instanceof SparkCli)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("spark_close" + mm.getMessage("function.paramTypeError"));
		}
		
		((SparkCli)client).close();
		return null;
	}
}
