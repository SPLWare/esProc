package com.scudata.lib.joinquant.function;

import java.util.HashMap;
import java.util.Map;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ImPricePeriod extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}
	//get_price_period(token,code,unit,date,end_date,fq_ref_date)
	public Object doQuery(Object[] objs){
		try {
			if (objs.length<5){
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant " + mm.getMessage("add param error"));
			}
			String tocken = null;
			String code = null;
			String unit = null;
			String date = null;
			String end_date = null;
			
			if (objs[0] instanceof String){
				tocken = objs[0].toString();
			}
			if (objs[1] instanceof String){
				code = objs[1].toString();
			}
			if (objs[2] instanceof String){
				unit = objs[2].toString();
			}
			if (objs[3] instanceof String){
				date = objs[3].toString();
			}
			if (objs[4] instanceof String){
				end_date = objs[4].toString();
			}
			
			if (tocken==null || code == null || unit == null || date == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant get_price_period " + mm.getMessage("function.missingParam"));
			}
	
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("method", "get_price_period");
			paramMap.put("token", tocken);
			paramMap.put("code", code);
			paramMap.put("unit", unit);
			paramMap.put("date", date);
			paramMap.put("end_date", end_date);
			
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
