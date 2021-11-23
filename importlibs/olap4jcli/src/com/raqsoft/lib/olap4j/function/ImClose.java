package com.raqsoft.lib.olap4j.function;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

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
