package com.raqsoft.lib.hive.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

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
