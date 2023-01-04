package com.scudata.dw;

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
	
	public void initExp() {
		Or and = new Or();
		and.setLeft(left.exp.getHome());
		and.setRight(right.exp.getHome());
		exp = new Expression(and);
	}
}