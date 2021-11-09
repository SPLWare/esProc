package com.raqsoft.lib.redis.function;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;

public class RedisRandomKey extends RedisBase {
	public Node optimize(Context ctx) {
		return super.optimize(ctx);
	}
	
	public Object doQuery( Object[] objs){
		m_colNames=new String[]{"Value"}; //for columns		

		String val= m_jedisTool.randomKey();

		Table table = new Table(m_colNames);
		table.newLast(new Object[]{val});
		
		return table;
	}
	
}
