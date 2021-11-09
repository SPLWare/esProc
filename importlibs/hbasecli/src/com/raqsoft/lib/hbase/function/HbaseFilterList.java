package com.raqsoft.lib.hbase.function;

import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;

public class HbaseFilterList extends Function {
	public FilterList list = null;
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {		
		IParam param = this.param;
		if (param == null ) {
			throw new RQException( "comparator param is null ");
		}
		
		FilterList list = null;
		String option = getOption();
		if (option!=null && option.equals("o")){ // 1. nullComparator
			list = new FilterList(FilterList.Operator.MUST_PASS_ONE);
		}else{
			list = new FilterList(FilterList.Operator.MUST_PASS_ALL);
		}
		
		Object obj = null;
		int size = param.getSubSize();
		// only one filter
		if (size == 0){
			if ((obj=ImUtils.checkValidDataType(param, ctx, "Filter"))==null){
				throw new RQException( "Param: " + obj + " not valid filter");
			}else{
				list.addFilter((Filter)obj);
			}
		}else{ //for multiple filters
			for(int i=0; i<size; i++){
				//System.out.println("filterList "+ i +" " +param.getSub(i));
				if ((obj=ImUtils.checkValidDataType(param.getSub(i), ctx, "Filter"))==null){
					throw new RQException( "Param: " + obj + " not valid filter");
				}else{
					list.addFilter((Filter)obj);
				}
			}
		}
		
		return list;
	}	
}
