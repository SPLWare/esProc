package com.raqsoft.lib.hbase.function;

import org.apache.hadoop.hbase.filter.*;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;

public class HbaseFilter extends Function {
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;		
		if (param == null ) {
			throw new RQException("hbase filter param is null");
		}

		int size = param.getSubSize();
		String filterName = "";
		Object obj = null;
		if (size == 0){
			if ((obj=ImUtils.checkValidDataType(param, ctx, "String"))==null){
				throw new RQException("Filter param: " +obj+" is unvalid");
			}
			filterName = (String)obj;
			if (filterName.compareToIgnoreCase("FirstKeyOnlyFilter")==0){
				return new FirstKeyOnlyFilter();
			}else if (filterName.compareToIgnoreCase("KeyOnlyFilter")==0){
				return new KeyOnlyFilter();
			}else{
				throw new RQException("FilterName: " +filterName+" is not existed");
			}
		}else{
			ParseFilter pf = new ParseFilter(ctx, param);
			Object o = pf.calculate();
			return o;
		}
	}
	
	
}
