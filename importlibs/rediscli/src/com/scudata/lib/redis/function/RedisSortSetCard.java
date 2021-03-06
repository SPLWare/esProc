package com.scudata.lib.redis.function;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;

public class RedisSortSetCard extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		m_colNames = new String[]{"Value"}; //for columns
		
		if (objs.length != 1){
			throw new RQException("redis zcard param size " + objs.length + " is not 1");
		}else{
			Long l = m_jedisTool.zZCard(objs[0].toString());
			if (l!=null){
				Table table = new Table(m_colNames);
				table.newLast(new Object[]{l});
				return table;
			}
		}
		
		return null;		
	}
}
