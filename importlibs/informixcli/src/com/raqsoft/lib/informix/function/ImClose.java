package com.raqsoft.lib.informix.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.informix.helper.IfxConn;
import com.raqsoft.resources.EngineMessage;

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
