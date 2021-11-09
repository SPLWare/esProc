package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.List;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisHashLen extends RedisBase {
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
		if (objs.length != 1){
			throw new RQException("redis strlen param size " + objs.length + " is not 1");
		}else{
			Long ll = m_jedisTool.hSize(objs[0].toString());
			//System.out.println("hlen length = " + ll);
			if (ll==null) return null;
			ls.add(Long.toString(ll));	
		}

		return toTable(ls.toArray());
	}
}
