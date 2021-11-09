package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisHashGet extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}
	
	public Object doQuery( Object[] objs){
		List<Object> ls = null;
		if (objs.length == 1){ //for hgetAll
			Map<Object, Object > m = m_jedisTool.hGetAll(objs[0].toString());
			if (m==null) return null;
			
			m_colNames = Utils.objectArrayToStringArray(m.keySet().toArray());
			return toTable(m.values().toArray());
		}else if (objs.length == 2){ //for hget
			Object[] os = new Object[1];
			os[0] = objs[1];
			super.doQuery(os); //for columns
			ls = new ArrayList<Object>();
			ls.add(m_jedisTool.hGet(objs[0].toString(), objs[1].toString()));
		}else if (objs.length>2){//for hmget	
			ls = new ArrayList<Object>();
			Object[] os = new Object[objs.length-1];
			for(int i=1; i<objs.length; i++){
				os[i-1] = objs[i];
			}
			super.doQuery(os); //for columns
			ls = m_jedisTool.hMultiGet(objs[0].toString(), Utils.objectArrayToList(os));			
		}else{
			throw new RQException("redis hget param " + objs.length + " is not right");
		}
		if (ls==null) return null;
		return toTable(ls.toArray());
	}
}
