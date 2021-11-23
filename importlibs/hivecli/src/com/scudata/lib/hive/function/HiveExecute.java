package com.scudata.lib.hive.function;

import com.scudata.dm.Context;
import com.scudata.expression.Node;
import com.scudata.lib.hive.function.HiveFunction;

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
