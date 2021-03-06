package com.scudata.lib.redis.function;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;

public class RedisHashHasKey extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string","string"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		m_colNames=new String[]{"Value"}; ; //for columns
		
		if (objs.length != 2){
			throw new RQException("redis strlen param size is not 2");
		}else{			
			Boolean bHas = m_jedisTool.hExists(objs[0].toString(), objs[1].toString());
			Table table = new Table(m_colNames);
			table.newLast(new Object[]{bHas});
			
			return table;
		}
	}
}
