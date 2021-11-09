package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.List;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisSortSetScore extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		List<String> ls = new ArrayList<String>();
		if (objs.length != 2){
			throw new RQException("redis zscore param size " + objs.length + " is not 2");
		}else{
			Object os[] = new Object[1];
			os[0]=objs[0]+"_"+objs[1];
			super.doQuery(os); //for columns
			
			Double l = m_jedisTool.zScore(objs[0].toString(),objs[1].toString());
			if (l!=null){
				ls.add(Double.toString(l));	
			}
		}

		return toTable(ls.toArray());
	}
}
