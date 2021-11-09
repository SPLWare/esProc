package com.raqsoft.lib.hbase.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class HbaseClose extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	// Õ∑≈¡¨Ω”
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hbase_close" + mm.getMessage("function.missingParam"));
		}

		Object client = param.getLeafExpression().calculate(ctx);
		if (!(client instanceof HbaseDriverCli)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hbase_close" + mm.getMessage("function.paramTypeError"));
		}
		
		((HbaseDriverCli)client).close();
		return null;
	}
}
