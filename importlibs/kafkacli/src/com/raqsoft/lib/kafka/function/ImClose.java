package com.raqsoft.lib.kafka.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class ImClose extends Function {
	
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(ImConnection.m_prjName + mm.getMessage("function.missingParam"));
		}

		Object client = param.getLeafExpression().calculate(ctx);
		if ((client instanceof ImConnection)) {
			ImConnection conn = ((ImConnection)client);
			conn.close();
		}else{
			MessageManager mm = EngineMessage.get();
			throw new RQException(ImConnection.m_prjName + mm.getMessage("function.paramTypeError"));
		}
		
		return null;
	}
}
