package com.scudata.lib.joinquant.function;

import java.util.HashMap;
import java.util.Map;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ImAllTradeDays extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}
	//get_all_trade_days (token)
	public Object doQuery(Object[] objs){
		try {
			if (objs.length!=1){
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant " + mm.getMessage("add param error"));
			}
			String tocken = null;
			
			if (objs[0] instanceof String){
				tocken = objs[0].toString();
			}
					
			if (tocken==null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant get_all_trade_days " + mm.getMessage("function.missingParam"));
			}
	
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("method", "get_all_trade_days");
			paramMap.put("token", tocken);
			
			String[] body = JQNetWork.GetNetArrayData(paramMap);
			Table tbl = DataType.toTable(body, new String[] {"day"});
			return tbl;
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
}
