package com.raqsoft.lib.spark.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

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
