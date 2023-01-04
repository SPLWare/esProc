package com.scudata.lib.joinquant.function;

import java.util.HashMap;
import java.util.Map;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

public class ImFundamentals extends ImFunction {
	public Node optimize(Context ctx) {
		return this;
	}
	//get_fundamentals(token,table,columns,code,date,count)
	public Object doQuery(Object[] objs){
		try {
			if (objs.length<4){
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant " + mm.getMessage("add param error"));
			}
			String tocken = null;
			String table = null;
			String columns = null;
			String code = "";
			String date = null;
			
			if (objs[0] instanceof String){
				tocken = objs[0].toString();
			}
			if (objs[1] instanceof String){
				table = objs[1].toString();
			}
			Map<String, Object> paramMap = new HashMap<>();
			if (objs[2]!=null && objs[2] instanceof String){
				columns = objs[2].toString();
				paramMap.put("columns", columns);
			}
				
			if (objs[3] instanceof String){
				code = objs[3].toString();
			}else if(objs[3] instanceof Sequence) {
				Sequence seq = (Sequence)objs[3];
				for(int n=0; n<seq.length(); n++) {
					code+=seq.get(n+1).toString()+",";
				}
				code = code.substring(0,code.length()-1);
			}
			
			if (objs[4] instanceof String){
				date = objs[4].toString();
			}
			
			if (tocken==null ||table==null|| code == null || date == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinquant get_fundamentals " + mm.getMessage("function.missingParam"));
			}
			
			paramMap.put("method", "get_fundamentals");
			paramMap.put("token", tocken);
			paramMap.put("table", table);
			paramMap.put("code", code);
			paramMap.put("date", date);
			
			if (objs.length>=6 && objs[5] instanceof Integer){
				int count = Integer.parseInt(objs[5].toString());
				paramMap.put("count", count);
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
