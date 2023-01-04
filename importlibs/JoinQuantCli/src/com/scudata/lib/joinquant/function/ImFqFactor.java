package com.scudata.lib.joinquant.function;

import java.util.HashMap;
import java.util.Map;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ImFqFactor extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}
	//get_fq_factor(token,code,fq,date,end_date)
	public Object doQuery(Object[] objs){
		try {
			if (objs.length<3){
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant " + mm.getMessage("add param error"));
			}
			String tocken = null;
			String code = null;
			String fq = null;

			
			if (objs[0] instanceof String){
				tocken = objs[0].toString();
			}
			if (objs[1] instanceof String){
				code = objs[1].toString();
			}
			if (objs[2] instanceof String){
				fq = objs[2].toString();
			}
						
			if (tocken==null || code == null || fq == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant get_fq_factor " + mm.getMessage("function.missingParam"));
			}
	
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("method", "get_fq_factor");
			paramMap.put("token", tocken);
			paramMap.put("code", code);
			paramMap.put("fq", fq);
			
			if (objs.length>=4 && objs[3] instanceof String){
				paramMap.put("date", objs[3].toString());
			}
			
			if (objs.length>=5 && objs[4] instanceof String){
				paramMap.put("end_date", objs[4].toString());
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
