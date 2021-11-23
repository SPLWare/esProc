package com.scudata.lib.redis.function;

import java.util.ArrayList;
import java.util.List;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Node;

public class RedisListIndex extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string","int"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		Object os[] = new Object[1];
		os[0]=objs[0];
		super.doQuery(os); //for columns
		
		List<String> ls = new ArrayList<String>();
		if (objs.length != 2){
			throw new RQException("redis hashRange param size " + objs.length + " is not 2");
		}else{
			String s = m_jedisTool.lIndex(objs[0].toString(), Integer.parseInt(objs[1].toString()) );
			ls.add(s);	
		}
		
		return toTable(ls.toArray());
	}
}
