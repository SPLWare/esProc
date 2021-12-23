package com.scudata.lib.dynamodb.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// dyna_close(fp)
public class ImClose extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("dyna_close" + mm.getMessage("function.missingParam"));
		}

		Object obj = param.getLeafExpression().calculate(ctx);
		if(obj==null) return null;
		
		if (!(obj instanceof ImOpen)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("dyna_close" + mm.getMessage("function.paramTypeError"));
		}
		
		((ImOpen)obj).close();
		
		return null;
	}
}
