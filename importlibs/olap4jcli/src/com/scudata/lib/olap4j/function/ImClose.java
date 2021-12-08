package com.scudata.lib.olap4j.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// olap_close(hd)
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
		if (!(client instanceof MdxQueryUtil)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ali_close" + mm.getMessage("function.paramTypeError"));
		}
		
		((MdxQueryUtil)client).close();
		return null;
	}
}
