package com.scudata.lib.joinquant.function;

import java.util.HashMap;
import java.util.Map;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ImRunQuery extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}
	//jq_query(token,table,columns, conditions, count);
	public Object doQuery(Object[] objs){
		try {
			if (objs.length<3){
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant query " + mm.getMessage("add param error"));
			}
			String tocken = null;
			String table = null;
			
			if (objs[0] instanceof String){
				tocken = objs[0].toString();
			}
			if (objs[1] instanceof String){
				table = objs[1].toString();
			}
									
			if (tocken==null || table == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant run_query " + mm.getMessage("function.missingParam"));
			}
	
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("method", "run_query");
			paramMap.put("token", tocken);
			paramMap.put("table", table);
			//columns
			if (objs.length>=3 && objs[2] instanceof String){
				paramMap.put("columns", objs[2].toString());
			}
			//conditions
			if (objs.length>=4 && objs[3] instanceof String){
				paramMap.put("conditions", objs[3].toString());
			}
			//count
			if (objs.length>=5 && objs[4] instanceof Integer){
				paramMap.put("count", Integer.parseInt(objs[4].toString()));
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
