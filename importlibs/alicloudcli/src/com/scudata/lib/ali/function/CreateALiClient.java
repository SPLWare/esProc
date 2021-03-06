package com.scudata.lib.ali.function;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

// ali_client(endpoint, accessKeyId, accessKeySecret, instanceName)
public class CreateALiClient extends Function {
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ali_client" + mm.getMessage("function.missingParam"));
		}

		if (param.getSubSize() != 4) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ali_client" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub0 = param.getSub(0);
		IParam sub1 = param.getSub(1);
		IParam sub2 = param.getSub(2);
		IParam sub3 = param.getSub(3);
		
		if (sub0 == null || sub1 == null || sub2 == null || sub3 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ali_client" + mm.getMessage("function.invalidParam"));
		}
		
		Object endpoint = sub0.getLeafExpression().calculate(ctx);
		Object keyId = sub1.getLeafExpression().calculate(ctx);
		Object keySecret = sub2.getLeafExpression().calculate(ctx);
		Object instanceName = sub3.getLeafExpression().calculate(ctx);
		
		if (!(endpoint instanceof String) || !(keyId instanceof String) || 
				!(keySecret instanceof String) || !(instanceName instanceof String)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ali_client" + mm.getMessage("function.paramTypeError"));
		}
		
		return new ALiClient((String)endpoint, (String)keyId, (String)keySecret, (String)instanceName);
	}
}
