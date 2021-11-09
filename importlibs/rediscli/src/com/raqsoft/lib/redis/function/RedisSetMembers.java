package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisSetMembers extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}

	public Object doQuery( Object[] objs){
		super.doQuery(objs); //for columns
		
		if (objs.length == 1){
			Set<String> set = m_jedisTool.sMembers(objs[0].toString());
			
			Object[] os = null;
			if (set.size()>0){
				List<Object[]> list = new ArrayList<Object[]>();
				for( Iterator<String> it = set.iterator(); it.hasNext(); ){  
					os = new Object[1];
					os[0] = it.next().toString();
		            list.add(os);            
		        } 
				return toTable(list);
			}else{
				return null;
			}
		}else{
			throw new RQException("redis smembers param size " + objs.length + " is not 1");		
		}
	}
}
