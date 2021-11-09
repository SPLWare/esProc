package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisSetScan extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string", "string"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		int count = 1000;
		if (objs.length>=3 && objs[2] instanceof Integer){
			count = Integer.parseInt(objs[2].toString());
		}
		
		ScanOptions options = new ScanOptions.ScanOptionsBuilder().match(objs[1].toString()).count(count).build();
		m_colNames = new String[]{"Value"};
		List<Object[]> ls = new ArrayList<Object[]>();
		Cursor<String> cursor = m_jedisTool.sScan(objs[0].toString(), options);
		while (cursor.hasNext()){
			ls.add(new String[]{cursor.next()});
		}
		
		return toTable(ls);
	}
}


