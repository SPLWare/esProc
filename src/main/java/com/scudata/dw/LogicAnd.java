package com.scudata.dw;

import com.scudata.array.IArray;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.operator.And;

/**
 * 与计算过滤器类
 * @author runqian
 *
 */
public class LogicAnd extends IFilter {
	private IFilter left;
	private IFilter right;
	
	public LogicAnd(IFilter left, IFilter right) {
		super(left.column, left.priority);
		this.left = left;
		this.right = right;
	}

	public LogicAnd(IFilter left, IFilter right, String columnName) {
		this.columnName = columnName;
		priority = left.priority;
		this.left = left;
		this.right = right;
	}

	public IFilter getLeft() {
		return left;
	}

	public IFilter getRight() {
		return right;
	}

	public boolean match(Object value) {
		return left.match(value) && right.match(value);
	}
	
	public boolean match(Object minValue, Object maxValue) {
		return left.match(minValue, maxValue) && right.match(minValue, maxValue);
	}
	
	public IArray calculateAll(Context ctx) {
		IArray leftResult = left.calculateAll(ctx);
		return right.calculateAnd(ctx, leftResult);
	}
	
	public IArray calculateAnd(Context ctx, IArray leftResult) {
		IArray tempResult = left.calculateAnd(ctx, leftResult);
		return right.calculateAnd(ctx, tempResult);
	}
	
	public void initExp() {
		And and = new And();
		left.initExp();
		right.initExp();
		and.setLeft(left.exp.getHome());
		and.setRight(right.exp.getHome());
		exp = new Expression(and);
	}
}