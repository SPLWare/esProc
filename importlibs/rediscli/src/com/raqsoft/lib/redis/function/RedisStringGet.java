package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.List;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisStringGet extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		super.doQuery(objs); //for columns
		
		List<String> ls = new ArrayList<String>();
		if (objs.length == 1){
			ls.add(m_jedisTool.get(objs[0].toString()));
		}else{
			List<String> params = new ArrayList<String>();
			for(Object o:objs){
				params.add(o.toString());
			}
			ls = m_jedisTool.multiGet(params);			
		}
		if (ls!=null){
			return toTable(ls.toArray());
		}else{
			return null;
		}
	}
}


