package com.scudata.lib.redis.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Node;

public class RedisKeys extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}


	public Object doQuery( Object[] objs){
		super.doQuery(objs); //for columns		
		
		if (objs.length == 1){
			Set<String> set = m_jedisTool.keys(objs[0].toString());

			Object[] os = null;
			if (set.size()>0){
				List<Object[]> list = new ArrayList<Object[]>();
				for( Iterator<String> it = set.iterator();  it.hasNext(); ){  
					os = new Object[1];
					os[0] = it.next().toString();
		            list.add(os);            
		        } 
				return toTable(list);
			}else{
				return null;
			}
		}else{
			throw new RQException("redis keys param size is not 1");		
		}
	}
}
