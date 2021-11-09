package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.List;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisStringGetRange extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string","int", "int"};
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
		if (objs.length != 3){
			throw new RQException("redis getRange param size " + objs.length + " is not 3");
		}else{
			String s = m_jedisTool.getRange(objs[0].toString(), Integer.parseInt(objs[1].toString()),
					 Integer.parseInt(objs[2].toString()));
			if (s!=null){
				ls.add(s);	
			}
		}
		
		return toTable(ls.toArray());
	}
}
