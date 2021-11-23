package com.scudata.lib.informix.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.lib.informix.helper.IfxConn;
import com.scudata.resources.EngineMessage;

// ifx_close(ali_client)
public class ImClose extends Function {
	
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ali_close" + mm.getMessage("function.missingParam"));
		}

		Object client = param.getLeafExpression().calculate(ctx);
		if ((client instanceof IfxConn)) {
			((IfxConn)client).close();
		}else{
			MessageManager mm = EngineMessage.get();
			throw new RQException("ali_close" + mm.getMessage("function.paramTypeError"));
		}
		
		return null;
	}
}
