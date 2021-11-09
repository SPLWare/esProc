package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Node;

public class RedisSetInter extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		Object os[] = new Object[1];
		os[0]=objs[0];
		super.doQuery(os); //for columns
		
		if (objs.length != 2){
			throw new RQException("redis sunion param size is not 2");
		}else{
			Set<String> set = null;
			if (objs[1] instanceof String){
				set = m_jedisTool.sIntersect(objs[0].toString(), objs[1].toString());
				if (set!=null && set.size()>0){
					List<Object[]> list = new ArrayList<Object[]>();
					for( Iterator<String> it = set.iterator();  it.hasNext(); ){  
						os = new Object[1];
						os[0] = it.next().toString();
			            list.add(os);            
			        } 
					return toTable(list);				
				}
			}else if(objs[1] instanceof Sequence){
				Sequence seq = (Sequence)objs[1];
				List<String> ls=new ArrayList<String>();
				for(int n=0; n<seq.length(); n++){
					ls.add(seq.get(n+1).toString());
				}
				set = m_jedisTool.sIntersect(objs[0].toString(), ls);
				if (set!=null && set.size()>0){
					List<Object[]> list = new ArrayList<Object[]>();
					for( Iterator<String> it = set.iterator();  it.hasNext(); ){  
						os = new Object[1];
						os[0] = it.next().toString();
			            list.add(os);            
			        } 
					return toTable(list);
				}
			}
		}

		return null;
	}
}
