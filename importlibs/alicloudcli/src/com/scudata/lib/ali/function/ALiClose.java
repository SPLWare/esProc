package com.scudata.lib.ali.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// ali_close(ali_client)
public class ALiClose extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ali_close" + mm.getMessage("function.missingParam"));
		}

		Object client = param.getLeafExpression().calculate(ctx);
		if (!(client instanceof ALiClient)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ali_close" + mm.getMessage("function.paramTypeError"));
		}
		
		((ALiClient)client).close();
		return null;
	}
}
