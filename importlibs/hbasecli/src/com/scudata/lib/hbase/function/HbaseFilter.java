package com.scudata.lib.hbase.function;

import org.apache.hadoop.hbase.filter.*;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;

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
