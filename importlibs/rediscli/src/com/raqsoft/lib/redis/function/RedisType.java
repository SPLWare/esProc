package com.raqsoft.lib.redis.function;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.connection.DataType;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;

public class RedisType extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		super.doQuery(objs); //for columns
		
		
		if (objs.length != 1){
			throw new RQException("redis type param size " + objs.length + " is not 1");
		}else{
			List<String> ls = new ArrayList<String>();
			DataType s = m_jedisTool.type(objs[0].toString());
			if (s!=null){
				if (s==DataType.STRING){
					ls.add("String");	
				}else if (s==DataType.LIST){
					ls.add("List");	
				}else if (s==DataType.SET){
					ls.add("Set");	
				}else if (s==DataType.ZSET){
					ls.add("Zset");	
				}else if (s==DataType.HASH){
					ls.add("Hash");	
				}else if (s==DataType.STREAM){
					ls.add("Stream");	
				}else if (s==DataType.NONE){
					ls.add("None");	
				}else {
					ls.add("String");	
				}
			}
			return toTable(ls.toArray());
		}
	}
}
