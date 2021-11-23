package com.scudata.lib.redis.function;

import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;

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
