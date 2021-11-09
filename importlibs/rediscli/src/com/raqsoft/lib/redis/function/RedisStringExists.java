package com.raqsoft.lib.redis.function;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisStringExists extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		super.doQuery(objs); //for columns

		if (objs.length == 1){
			boolean b = m_jedisTool.hasKey(objs[0].toString());
			return toTable(new Object[]{b});
		}
		
		return null;		
	}
}


