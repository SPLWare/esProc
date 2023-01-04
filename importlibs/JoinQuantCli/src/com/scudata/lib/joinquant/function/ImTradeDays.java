package com.scudata.lib.joinquant.function;

import java.util.HashMap;
import java.util.Map;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ImTradeDays extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}
	//get_trade_days(token,code,date,end_date)
	public Object doQuery(Object[] objs){
		try {
			if (objs.length<2){
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant " + mm.getMessage("add param error"));
			}
			String tocken = null;
			String date = null;
			
			if (objs[0] instanceof String){
				tocken = objs[0].toString();
			}
			if (objs[1] instanceof String){
				date = objs[1].toString();
			}
					
			if (tocken==null || date == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant get_trade_days " + mm.getMessage("function.missingParam"));
			}
	
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("method", "get_trade_days");
			paramMap.put("token", tocken);
			paramMap.put("date", date);
			
			if (objs.length>=3 && objs[2] instanceof String){
				paramMap.put("end_date", objs[2].toString());
			}
						
			String[] body = JQNetWork.GetNetArrayData(paramMap);
			Table tbl = DataType.toTable(body, new String[] {"day"});
			return tbl;
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
}
