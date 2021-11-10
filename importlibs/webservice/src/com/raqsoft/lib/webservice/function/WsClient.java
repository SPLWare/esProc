package com.raqsoft.lib.webservice.function;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class WsClient extends Function {


	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		//Logger.debug("come in ws_client");
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ws_client" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		//Logger.debug("size " + size);
		String url = null;
		if (size == 0) {
			Expression exp = param.getLeafExpression();
			if (exp == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ws_client" + mm.getMessage("function.invalidParam"));
			}
			//Logger.debug("size " + size);
			url = exp.calculate(ctx).toString();
		} else {
			url = param.getSub(0).getLeafExpression().calculate(ctx).toString();
		}
		//Logger.debug("url " + url);
		
		return new WsClientImpl(url);
	}

}
