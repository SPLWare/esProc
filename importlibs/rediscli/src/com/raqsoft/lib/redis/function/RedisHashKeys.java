package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisHashKeys extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}
	
	public Object doQuery( Object[] objs){
		super.doQuery(objs); //for columns
		
		if (objs.length != 1){
			throw new RQException("redis strlen param size " + objs.length + " is not 1");
		}else{			
			Set<Object> set = m_jedisTool.hKeys(objs[0].toString());
			//System.out.println("hkeys size = " + set.size());
			Object[] os = null;
			if (set.size()>0){
				List<Object[]> list = new ArrayList<Object[]>();
				for( Iterator<Object> it = set.iterator();  it.hasNext(); ){  
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
