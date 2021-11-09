package com.raqsoft.lib.redis.function;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;

public class RedisPersist extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}
	
	public Object doQuery( Object[] objs){
		super.doQuery(objs); //for columns		

		boolean bPersist = m_jedisTool.persist(objs[0].toString());

		Table table = new Table(m_colNames);
		table.newLast(new Object[]{bPersist});
		
		return table;
	}
	
}
