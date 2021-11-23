package com.scudata.lib.redis.function;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;

public class RedisSortSetCount extends RedisBase {
	protected String m_paramTypes2[];
	
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string","double","double"};
		return super.optimize(ctx);
	}

	public Object doQuery( Object[] objs){
		
		if (objs.length != 3){
			throw new RQException("redis strlen param size " + objs.length + " is not 3");
		}else{
			m_colNames=new String[]{"Value"}; //for columns
			
			if (objs[1] instanceof Double){
				Long ll = m_jedisTool.zCount(objs[0].toString(), Double.parseDouble(objs[1].toString()), 
						Double.parseDouble(objs[2].toString()) );
				if (ll!=null){
					Table table = new Table(m_colNames);
					table.newLast(new Object[]{ll});
					
					return table;
				}
			}
		}
		
		return null;
	}
}
