package com.scudata.lib.hive.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// hive_close(hive_client)
public class HiveClose extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hive_close" + mm.getMessage("function.missingParam"));
		}

		Object client = param.getLeafExpression().calculate(ctx);
		if (!(client instanceof HiveDriverCli)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hive_close" + mm.getMessage("function.paramTypeError"));
		}
		
		((HiveDriverCli)client).close();
		return null;
	}
}
