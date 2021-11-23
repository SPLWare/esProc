package com.scudata.lib.redis.function;

import java.util.ArrayList;
import java.util.List;

import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Node;

public class RedisSetIsMember extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string","string"};
		return super.optimize(ctx);
	}

	public Object doQuery( Object[] objs){
		List<String> ls = new ArrayList<String>();
		if (objs.length != 2){
			throw new RQException("redis IsMember param size is not 2");
		}else{
			//super.doQuery(objs); //for columns
			m_colNames = new String[1];
			m_colNames[0] = objs[0].toString()+"_"+objs[1].toString();
			Boolean bool = m_jedisTool.sIsMember(objs[0].toString(),objs[1].toString());
			if (bool!=null){
				ls.add(Boolean.toString(bool));	
			}
		}
		
		return toTable(ls.toArray());
	}
}
