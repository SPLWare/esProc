package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.List;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisRun extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string","string"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		//super.doQuery(objs); //for columns
		List<Object> ls = new ArrayList<Object>();
		if (objs.length<2){
			throw new RQException("redis_run: parameter error!");
		}
		String method = objs[0].toString();
		
		
		if (method.equals("exists")){
			m_colNames = new String[]{method};
			ls.add(m_jedisTool.hasKey(objs[1].toString()));
		}else if (method.equals("del")){
			m_colNames = new String[]{method};
			m_jedisTool.delete(objs[1].toString());
			ls.add("1");
		}else if (method.equals("expire")){
			m_colNames = new String[]{"time"};
			ls.add(m_jedisTool.expire(objs[1].toString(), Integer.parseInt(objs[2].toString())));
		}else if (method.equals("ttl")){
			m_colNames = new String[]{"time"};
			ls.add( m_jedisTool.getExpire(objs[1].toString()) );
		}else if (method.equals("persist")){
			m_colNames = new String[]{"time"};
			ls.add( m_jedisTool.persist(objs[1].toString()) );
		}
		
		return toTable(ls.toArray());
	}
}


