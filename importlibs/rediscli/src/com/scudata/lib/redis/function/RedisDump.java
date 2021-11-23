package com.scudata.lib.redis.function;

import java.io.UnsupportedEncodingException;
import com.scudata.dm.Context;
import com.scudata.dm.Table;
import com.scudata.expression.Node;

public class RedisDump extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}
	
	public Object doQuery( Object[] objs){
		super.doQuery(objs); //for columns		

		byte[] bt = m_jedisTool.dump(objs[0].toString());

		Table table = new Table(m_colNames);
		table.newLast(new Object[]{new String(bt)});
		
		return table;
	}
	
}
