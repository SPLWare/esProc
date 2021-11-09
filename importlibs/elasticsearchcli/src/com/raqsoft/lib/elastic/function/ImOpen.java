package com.raqsoft.lib.elastic.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.elastic.helper.RestConn;
import com.raqsoft.resources.EngineMessage;

// ImOpen(host, ..., user:passwd)
public class ImOpen extends Function {
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("client" + mm.getMessage("function.missingParam"));
		}
		int nSize = param.getSubSize();
		Map<String, Object> map = new HashMap<String, Object>();
		if (param.getType()==IParam.Comma){
			Expression o = null;
			for(int i=0; i<nSize; i++){
				ArrayList<Expression> ls = new ArrayList<Expression>();	
				if (param.getSub(i).getType()==IParam.Colon){
					IParam subs = param.getSub(i);
					int nSubSize = subs.getSubSize();					
					for(int j=0; j<nSubSize; j++){
						IParam sub = subs.getSub(j);
						o = sub.getLeafExpression();
						ls.add(o);
					}
					parseParams(2, ls, map);
				}else{									
					param.getSub(i).getAllLeafExpression(ls);	
					parseParams(1, ls, map);
				}
			}
		}else{			
			ArrayList<Expression> ls = new ArrayList<Expression>();
			param.getAllLeafExpression(ls);	
			parseParams(1, ls, map);			
		}
		option = this.getOption();
		if (option!=null && option.indexOf("s")!=-1){
			map.put("scheme", true);
		}
		
		return new RestConn(ctx, map);
	}

	//nType: 1=>url, 2=>user:pwd
	protected void parseParams(int nType, ArrayList<Expression> ls, Map<String, Object> map){
		int nSize = ls.size();
		if (nSize==0) return;
		String s = null;
		int len = 0;
		if (nType==1){
			s = ls.get(0).toString();
			len = s.length();
			if (len > 2 && s.charAt(0) == '"' && s.charAt(len - 1) == '"') {
				s = s.substring(1, len - 1);
			}
			int idx = getHostIndex(map);
			map.put("host"+idx, s);
		}else if (nType==2){
			s = ls.get(0).toString();
			len = s.length();
			if (len > 2 && s.charAt(0) == '"' && s.charAt(len - 1) == '"') {
				s = s.substring(1, len - 1);
			}
			map.put("user", s);
			s = ls.get(1).toString();
			len = s.length();
			if (len > 2 && s.charAt(0) == '"' && s.charAt(len - 1) == '"') {
				s = s.substring(1, len - 1);
			}
			if (nSize>1){
				map.put("passwd", s);
			}
		}
	}
	
	private int getHostIndex(Map<String, Object> map){
		int nRet = 0;
		String s = "host";
		for(int i=1; i<100; i++){
			String key = s+i;
			if (!map.containsKey(key)){
				nRet = i;
				break;
			}
		}
		return nRet;
	}
}
