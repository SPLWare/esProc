package com.raqsoft.lib.sap.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

// sap_close(hive_client)
public class ImClose extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hive_close" + mm.getMessage("function.missingParam"));
		}

		Object client = param.getLeafExpression().calculate(ctx);
		if(client==null) return null;
		
		if (!(client instanceof RfcManager)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hive_close" + mm.getMessage("function.paramTypeError"));
		}
		
		((RfcManager)client).close();
		return null;
	}
}
