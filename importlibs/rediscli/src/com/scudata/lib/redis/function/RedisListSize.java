package com.scudata.lib.redis.function;

import java.util.ArrayList;
import java.util.List;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Node;

public class RedisListSize extends RedisBase {
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
			Long l = m_jedisTool.lSize(objs[0].toString());
			if (l!=null){
				ls.add(Long.toString(l));	
			}
		}
		
		return toTable(ls.toArray());
	}
}
