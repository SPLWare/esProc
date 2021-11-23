package com.scudata.lib.redis.function;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;

public class RedisSortSetSize extends RedisBase {
	protected String m_paramTypes2[];
	
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}

	public Object doQuery( Object[] objs){		
		if (objs.length != 1){
			throw new RQException("redis strlen param size is not 1");
		}else{
			m_colNames=new String[]{"Value"}; //for columns
			Long ll = m_jedisTool.zSize(objs[0].toString() );
			if (ll!=null){
				Table table = new Table(m_colNames);
				table.newLast(new Object[]{ll});
				
				return table;
			}	
		}
		
		return null;
	}
}
