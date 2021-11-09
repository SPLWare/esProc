package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.List;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisSortSetRank extends RedisBase {
	boolean m_bReverse = false;	//µπ≈≈–Ú
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string","string"};
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
		List<String> ls = new ArrayList<String>();
		if (objs.length != 2){
			throw new RQException("redis zrank param size " + objs.length + " is not 2");
		}else{
			Object os[] = new Object[1];
			os[0]=objs[0]+"_"+objs[1];
			super.doQuery(os); //for columns
			
			Long l;
			if (m_bReverse){
				l = m_jedisTool.zReverseRank(objs[0].toString(),objs[1]);
			}else{				
				l = m_jedisTool.zRank(objs[0].toString(),objs[1].toString());
			}
			
			if (l!=null){
				ls.add(Long.toString(l));	
				return toTable(ls.toArray());
			}
		}

		return null;
	}
}
