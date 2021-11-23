package com.scudata.lib.redis.function;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;

public class RedisListTrim extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string","long","long"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		m_colNames=new String[]{"Value"}; //for columns		
		
		if (objs.length < 2){
			throw new RQException("redis strlen param size " + objs.length + " is not 3");
		}else{
			m_jedisTool.lTrim(objs[0].toString(),
					Utils.objectToLong(objs[1]), Utils.objectToLong(objs[2]));
			
			Table table = new Table(m_colNames);
			table.newLast(new Object[]{1});
			
			return table;
		}		
	}
}
