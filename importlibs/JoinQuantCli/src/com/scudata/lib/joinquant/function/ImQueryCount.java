package com.scudata.lib.joinquant.function;

import java.util.HashMap;
import java.util.Map;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ImQueryCount extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object doQuery(Object[] objs){
		try {
			if (objs.length!=1){
				MessageManager mm = EngineMessage.get();
				throw new RQException("ImQueryCount " + mm.getMessage("add param error"));
			}
			String sToken = null;
			if (objs[0] instanceof String){
				sToken = objs[0].toString();
			}else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("ImQueryCount " + mm.getMessage("function.missingParam"));
			}
	
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("method", "get_query_count");
			paramMap.put("token", sToken);
			
			String ret = JQNetWork.SentPostBody(JQNetWork.mExecurl, paramMap);
			return Integer.parseInt(ret);
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
}
