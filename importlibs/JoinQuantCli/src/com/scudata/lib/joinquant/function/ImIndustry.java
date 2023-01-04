package com.scudata.lib.joinquant.function;

import java.util.HashMap;
import java.util.Map;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ImIndustry extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}
	//get_industry(token,code,date)
	public Object doQuery(Object[] objs){
		try {
			if (objs.length<2){
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant " + mm.getMessage("add param error"));
			}
			String tocken = null;
			String code = null;
			
			if (objs[0] instanceof String){
				tocken = objs[0].toString();
			}
			if (objs[1] instanceof String){
				code = objs[1].toString();
			}
					
			if (tocken==null || code == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant get_industry " + mm.getMessage("function.missingParam"));
			}
	
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("method", "get_industry");
			paramMap.put("token", tocken);
			paramMap.put("code", code);
						
			if (objs.length>=3 && objs[2] instanceof String){
				paramMap.put("date", objs[2].toString());
			}
						
			String[] body = JQNetWork.GetNetArrayData(paramMap);
			Table tbl = DataType.toTableOfString(body);
			return tbl;
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
}
