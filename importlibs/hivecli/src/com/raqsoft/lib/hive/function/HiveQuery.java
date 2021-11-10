package com.raqsoft.lib.hive.function;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.hive.function.HiveFunction;

public class HiveQuery extends HiveFunction {
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}

	public Object calculate(Context ctx) {
		Object o = super.calculate(ctx);
		if (option!=null && option.equals("x")){
			m_hiveBase.driver.close();
		}
		return o;		
	}
	
	public Object doQuery( Object[] objs){
		String sql = objs[0].toString();
	
		return m_hiveBase.selectData(sql);
	}
}
