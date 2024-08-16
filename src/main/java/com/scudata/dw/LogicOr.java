package com.scudata.dw;

import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.operator.Or;

/**
 * 或计算过滤器类
 * @author runqian
 *
 */
public class LogicOr extends IFilter {
	private IFilter left;
	private IFilter right;
	
	public LogicOr(IFilter left, IFilter right) {
		super(left.column, left.priority);
		this.left = left;
		this.right = right;
	}
	
	public LogicOr(IFilter left, IFilter right, String columnName) {
		this.columnName = columnName;
		priority = left.priority;
		this.left = left;
		this.right = right;
	}
	
	public boolean match(Object value) {
		return left.match(value) || right.match(value);
	}
	
	public boolean match(Object minValue, Object maxValue) {
		return left.match(minValue, maxValue) || right.match(minValue, maxValue);
	}
	
	public int isValueRangeMatch(Context ctx) {
		int ret = left.isValueRangeMatch(ctx);
		if (ret == 1) {
			return 1;
		} else if (ret == -1) {
			return right.isValueRangeMatch(ctx);
		} else {
			int ret2 = right.isValueRangeMatch(ctx);
			if (ret2 == 1) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
	public void initExp() {
		if (exp == null) {
			left.initExp();
			right.initExp();
			
			Or or = new Or();
			or.setLeft(left.exp.getHome());
			or.setRight(right.exp.getHome());
			exp = new Expression(or);
		}
	}
}