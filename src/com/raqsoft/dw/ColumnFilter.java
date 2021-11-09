package com.raqsoft.dw;

import com.raqsoft.util.Variant;

public class ColumnFilter extends IFilter {
	private Object rightValue;
	private int operator;

	public ColumnFilter(ColumnMetaData column, int priority, int operator, Object rightValue) {
		super(column, priority);
		this.operator = operator;
		this.rightValue = rightValue;
	}
	
	public ColumnFilter(String column, int priority, int operator, Object rightValue) {
		columnName = column;
		this.priority = priority;
		this.operator = operator;
		this.rightValue = rightValue;
	}
	
	public boolean match(Object value) {
		switch (operator) {
		case EQUAL:
			return Variant.isEquals(value, rightValue);
		case GREATER:
			return Variant.compare(value, rightValue, true) > 0;
		case GREATER_EQUAL:
			return Variant.compare(value, rightValue, true) >= 0;
		case LESS:
			return Variant.compare(value, rightValue, true) < 0;
		case LESS_EQUAL:
			return Variant.compare(value, rightValue, true) <= 0;
		default: //NOT_EQUAL:
			return !Variant.isEquals(value, rightValue);
		}
	}
	
	public boolean match(Object minValue, Object maxValue) {
		switch (operator) {
		case EQUAL:
			return Variant.compare(minValue, rightValue, true) <= 0 && Variant.compare(maxValue, rightValue, true) >= 0;
		case GREATER:
			return Variant.compare(maxValue, rightValue, true) > 0;
		case GREATER_EQUAL:
			return Variant.compare(maxValue, rightValue, true) >= 0;
		case LESS:
			return Variant.compare(minValue, rightValue, true) < 0;
		case LESS_EQUAL:
			return Variant.compare(minValue, rightValue, true) <= 0;
		default: //NOT_EQUAL:
			return !Variant.isEquals(minValue, rightValue) || !Variant.isEquals(maxValue, rightValue);
		}
	}
	
	public Object getRightValue() {
		return rightValue;
	}

	public void setRightValue(Object rightValue) {
		this.rightValue = rightValue;
	}

	public int getOperator() {
		return operator;
	}

	public void setOperator(int operator) {
		this.operator = operator;
	}

}