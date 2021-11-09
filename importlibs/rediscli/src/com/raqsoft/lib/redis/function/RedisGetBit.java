package com.raqsoft.lib.redis.function;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;

public class RedisGetBit extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string", "int"};
		return super.optimize(ctx);
	}
	
	public Object doQuery( Object[] objs){
		m_colNames=new String[]{"Value"}; //for columns		

		boolean bBit = m_jedisTool.getBit(objs[0].toString(), 
				Integer.parseInt(objs[1].toString()));

		Table table = new Table(m_colNames);
		table.newLast(new Object[]{bBit});
		
		return table;
	}
	
}
