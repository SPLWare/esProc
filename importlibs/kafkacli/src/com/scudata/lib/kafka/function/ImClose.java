package com.scudata.lib.kafka.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

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
