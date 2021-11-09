package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisHashVals extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		super.doQuery(objs); //for columns
		
		if (objs.length != 1){
			throw new RQException("redis strlen param size " + objs.length + " is not 1");
		}else{
			List<Object> ls = m_jedisTool.hValues(objs[0].toString());
			
			Object[] os = null;
			if (ls.size()>0){
				List<Object[]> list = new ArrayList<Object[]>();
				for( Iterator<Object> it = ls.iterator();  it.hasNext(); ){  
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
