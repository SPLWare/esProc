package com.scudata.lib.sap.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// sap_close(hive_client)
public class ImClose extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sap_close " + mm.getMessage("function.missingParam"));
		}

		Object client = param.getLeafExpression().calculate(ctx);
		if(client==null) return null;
		
		if (!(client instanceof RfcManager)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("sap_close " + mm.getMessage("function.paramTypeError"));
		}
		
		((RfcManager)client).close();
		return null;
	}
}
