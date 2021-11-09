package com.raqsoft.lib.redis.function;

import java.util.Date;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Table;
import com.raqsoft.expression.Node;

public class RedisExpire extends RedisBase {
	public Node optimize(Context ctx) {
		m_paramTypes = new String[]{"string"};
		return super.optimize(ctx);
	}
	
	public Object doQuery( Object[] objs){
		m_colNames=new String[]{"Value"};  //for columns		
		Table table = null;
		boolean bSuccess = false;
		if (objs.length==2 && objs[1] instanceof Integer){
			bSuccess = m_jedisTool.expire(objs[0].toString(), 
					Utils.objectToLong(objs[0]));
		}else if (objs.length==3 && objs[1] instanceof Integer && objs[1] instanceof Integer){
			bSuccess = m_jedisTool.expire(objs[0].toString(), Utils.objectToLong(objs[1]),
					Utils.intToEnum((Integer)objs[2]) );
		}else if (objs.length==2 && objs[1] instanceof Date){
			bSuccess = m_jedisTool.expireAt(objs[0].toString(), 
					(Date)objs[1]);
		}else if (objs.length==2 && objs[1] instanceof String){
			if (Utils.isRegExpMatch(objs[1].toString(), "yyyy-MM-dd")){
				bSuccess = m_jedisTool.expireAt(objs[0].toString(), 
						new Date(objs[1].toString()));
			}
		}
		
		table = new Table(m_colNames);
		table.newLast(new Object[]{bSuccess});
		return table;
	}

	
}
