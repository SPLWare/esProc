package com.raqsoft.lib.hive.function;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.hive.function.HiveFunction;

public class HiveCursor extends HiveFunction {
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		String sql = objs[0].toString();
		
		if (m_hiveBase.queryData(sql)){
			return new HiveICursor(m_hiveBase, m_ctx);
		}else{
			System.out.println("hive cursor run "+ sql +"sql false");
			return null;
		}
		
	}
}
