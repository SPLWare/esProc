package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisSortSetRange extends RedisBase {
	boolean m_bReverse = false;
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string","int","int"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		String option = getOption();
		// 1. nullComparator, param is null
		if (option!=null && option.equals("z")){
			m_bReverse = true;
		}
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		if (objs.length < 3){
			throw new RQException("redis zrange param size is less than 3");
		}else if (objs.length==3){
			Object os[] = new Object[1];
			m_colNames = new String[]{"Value"}; //for columns
			
			Set<String> set;
			if (m_bReverse){
				set = m_jedisTool.zReverseRange(objs[0].toString(), Utils.objectToLong(objs[1]), 
						Utils.objectToLong(objs[2]));
			}else{
				set = m_jedisTool.zRange(objs[0].toString(), Utils.objectToLong(objs[1]), 
							Utils.objectToLong(objs[2]));
			}
			if (set.size()>0){
				List<Object[]> list = new ArrayList<Object[]>();
				for( Iterator<String> it = set.iterator();  it.hasNext(); ){  
					os = new Object[1];
					os[0] = it.next().toString();
					//System.out.println("skey="+os[0]);
		            list.add(os);            
		        } 
				return toTable(list);
			}
		}else if (objs.length==4){
			if ("withscores".equalsIgnoreCase(objs[3].toString())){				
				Set<TypedTuple<String>> set;
				if (m_bReverse){
					set = m_jedisTool.zReverseRangeWithScores(objs[0].toString(), 
						Utils.objectToLong(objs[1]), Utils.objectToLong(objs[2]));
				}else{
					set = m_jedisTool.zRangeWithScores(objs[0].toString(), 
							Utils.objectToLong(objs[1]), Utils.objectToLong(objs[2]));
				}
						
				int len = set.size();
				if (len>0){
					Object os[] = new Object[2];
					os[0]=objs[0]+"_key";
					os[1]=objs[0]+"_val";
					super.doQuery(os); //for columns
					
					List<Object[]> list = new ArrayList<Object[]>();
					for(TypedTuple<String> tp:set){
						os = new Object[2];
						os[0] = tp.getValue();
						os[1] = tp.getScore();
						//System.out.println("dkey="+os[0]+" val="+os[1]);
			            list.add(os);   
					}
					return toTable(list);
				}
			}
		}
		
		return null;
	}
}
