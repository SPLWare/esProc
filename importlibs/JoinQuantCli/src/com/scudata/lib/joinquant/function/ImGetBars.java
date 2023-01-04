package com.scudata.lib.joinquant.function;

import java.util.HashMap;
import java.util.Map;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ImGetBars extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}
	
	//get_bars(token,code,unit,count,end_date,fq_ref_date)
	public Object doQuery(Object[] objs){
		try {
			if (objs.length<4){
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant " + mm.getMessage("add param error"));
			}
			String tocken = null;
			String code = null;
			int count = 0;
			String unit = null;
			
			if (objs[0] instanceof String){
				tocken = objs[0].toString();
			}
			if (objs[1] instanceof String){
				code = objs[1].toString();
			}
			
			if (objs[2] instanceof String){
				unit = objs[2].toString();
			}
				
			if (objs[3] instanceof Integer){
				count = Integer.parseInt(objs[3].toString());
			}
			
			if (tocken==null || code == null || unit == null || count == 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant get_bars " + mm.getMessage("function.missingParam"));
			}
	
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("method", "get_bars");
			paramMap.put("token", tocken);
			paramMap.put("code", code);
			paramMap.put("count", count);
			paramMap.put("unit", unit);
			
			if (objs.length>=5 && objs[4] instanceof String){
				paramMap.put("end_date", objs[4].toString());
			}
			if (objs.length>=6 && objs[5] instanceof String){
				paramMap.put("fq_ref_date", objs[5].toString());
			}
			
			String[] body = JQNetWork.GetNetArrayData(paramMap);
			Table tbl = DataType.toTable(body);
			return tbl;
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
}
