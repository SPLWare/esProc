package com.raqsoft.lib.redis.function;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;

public class RedisGetExpire extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}
	
	public Object doQuery( Object[] objs){
		m_colNames=new String[]{"Value"}; //for columns		
		Table table = null;
		long llong = 0;
		if (objs.length==1){
			llong = m_jedisTool.getExpire(objs[0].toString());
		}else if (objs.length==2 && objs[1] instanceof Integer){
			llong = m_jedisTool.getExpire(objs[0].toString(), 
					Utils.intToEnum((Integer)objs[1]) );
		}
		
		table = new Table(m_colNames);
		table.newLast(new Object[]{llong});
		return table;
	}
	
}
