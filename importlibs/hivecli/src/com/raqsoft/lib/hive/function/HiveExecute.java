package com.raqsoft.lib.hive.function;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.hive.function.HiveFunction;

public class HiveExecute extends HiveFunction {
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}

	public Object calculate(Context ctx) {
		return super.calculate(ctx);
	}
	
	public Object doQuery( Object[] objs){
		String sql = objs[0].toString();

		try {
			return m_hiveBase.execSql(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
}
