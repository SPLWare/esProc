package com.raqsoft.lib.webservice.function;

import java.util.ArrayList;

import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class WsCall  extends Function {

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ws_call" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		if (size < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ws_call" + mm.getMessage("function.invalidParam"));
		}
		
		Logger.debug("size : " + size);
		Object objs[] = new Object[size];
		WsClientImpl client = null;
		String service = null;
		String port = null;
		String func = null;
		ArrayList<String> values = new ArrayList<String>();
		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ws_call" + mm.getMessage("function.invalidParam"));
			}
//			Logger.debug(i+" : " + param.getSub(i));
//			Logger.debug(i+" : " + param.getSub(i).getType());
//			Logger.debug(i+" : " + param.getSub(i).getLeafExpression());
//			objs[i] = param.getSub(i).getLeafExpression().calculate(ctx);
			
			if (i==0) {
				client = (WsClientImpl)param.getSub(i).getLeafExpression().calculate(ctx);
				if (client == null) throw new RQException("first parameter is not ws_client");
			} else if (i == 1) {
				char type = param.getSub(i).getType();
				if (type == IParam.Colon) {
					if (param.getSub(i).getSubSize() != 3) throw new RQException("should like \"service:port:operation\"");
					service = param.getSub(i).getSub(0).getLeafExpression().calculate(ctx).toString();
					port = param.getSub(i).getSub(1).getLeafExpression().calculate(ctx).toString();
					func = param.getSub(i).getSub(2).getLeafExpression().calculate(ctx).toString();
				} else {
					func = param.getSub(i).getLeafExpression().calculate(ctx).toString();
					//service = param.getSub(i).getLeafExpression().calculate(ctx).toString();
				}
			} else {
				char type = param.getSub(i).getType();
				if (type == IParam.Colon) {
					String n = param.getSub(i).getSub(1).getLeafExpression().getIdentifierName();
					n = n.replaceAll("\"", "");
					values.add(param.getSub(i).getSub(0).getLeafExpression().calculate(ctx).toString()+":"+n);
				} else values.add(param.getSub(i).getLeafExpression().calculate(ctx).toString());
			}
		}
		
		try {
			return client.call(service, port, func, values.size()>0?values.toArray(new String[values.size()]):new String[]{});
		} catch (Exception e) {
			e.printStackTrace();
			throw new RQException("ws_call : " + e.getMessage());
		}
	}

}
