package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisSetRandMember extends RedisBase {
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
		
		if (objs.length < 1){
			throw new RQException("redis randMember param size " + objs.length + " too smaller ");
		}else{
			List<String> ls = new ArrayList<String>();
			
			if (objs.length==1){
				ls.add(m_jedisTool.sRandomMember(objs[0].toString()));
				return toTable(ls.toArray());
			}else{ //由一行多列转换成一列多行
				if (option!=null && option.contains("i")){//去重
					Set<String> set = m_jedisTool.sDistinctRandomMembers(objs[0].toString(), Long.parseLong(objs[1].toString()) );
					if (set!=null && set.size()>0){
						List<Object[]> list = new ArrayList<Object[]>();
						for( Iterator<String> it = set.iterator();  it.hasNext(); ){  
				            list.add( new Object[]{it.next().toString()});            
				        } 
						return toTable(list);				
					}
				}else{
					ls = m_jedisTool.sRandomMembers(objs[0].toString(), Long.parseLong(objs[1].toString()) );
					if (ls!=null && ls.size()>0){
						List<Object[]> list = new ArrayList<Object[]>();
						for( Iterator<String> it = ls.iterator();  it.hasNext(); ){  
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
}
