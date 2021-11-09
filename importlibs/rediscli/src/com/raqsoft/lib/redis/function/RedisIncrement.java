package com.raqsoft.lib.redis.function;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;

public class RedisIncrement extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}
	
	public Object doQuery( Object[] objs){
		m_colNames=new String[]{"Value"};; //for columns		
		Table table = new Table(m_colNames);
		long ll = 0;
		if (objs.length==1){
			ll = m_jedisTool.increment(objs[0].toString());
		}else if (objs.length==2 && objs[1] instanceof Integer){
			ll = m_jedisTool.increment(objs[0].toString(), 
					Utils.objectToLong(objs[1]));
		}else if (objs.length==2 && objs[1] instanceof Double){
			Double d = m_jedisTool.increment(objs[0].toString(), 
					Utils.objectToDouble(objs[1]));
			table.newLast(new Object[]{d});
			
			return table;
		}
		
		table.newLast(new Object[]{ll});
		
		return table;
	}
	
}
