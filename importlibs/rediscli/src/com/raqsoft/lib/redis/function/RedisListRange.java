package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisListRange extends RedisBase {
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

		if (objs.length != 3){
			throw new RQException("redis getRange param size " + objs.length + " is not 3");
		}else{
			List<String> ls = m_jedisTool.lRange(objs[0].toString(), Integer.parseInt(objs[1].toString()),
					 Integer.parseInt(objs[2].toString()));
			
			if (ls.size()>0){
				List<Object[]> list = new ArrayList<Object[]>();
				for( Iterator<String> it = ls.iterator();  it.hasNext(); ){  
					os = new Object[1];
					os[0] = it.next().toString();
		            list.add(os);            
		        } 
				return toTable(list);
			}else{
				return null;
			}
		}
		
		
	}
}
