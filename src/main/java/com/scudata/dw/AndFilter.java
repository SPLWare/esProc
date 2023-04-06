package com.scudata.dw;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.util.Variant;

/**
 * 基于与运算的列过滤器
 * @author runqian
 *
 */
public class AndFilter extends IFilter {
	private Number andValue;
	private Number rightValue;
	private long longValue = 0; // 如果andValue等于rightValue并且不是大整数则用longValue优化
	private int operator;

	/**
	 * Constructs a AndFilter for a column-stored table
	 * @param column
	 * @param priority
	 * @param operator
	 * @param andValue
	 * @param rightValue
	 */
	public AndFilter(ColumnMetaData column, int priority, int operator, Object andValue, Object rightValue) {
		super(column, priority);
		this.operator = operator;
		this.andValue = (Number) andValue;
		this.rightValue = (Number) rightValue;
		
		optimize();
	}
	
	public AndFilter(ColumnMetaData column, int priority, int operator, Object andValue, Object rightValue, Node node) {
		super(column, priority);
		this.operator = operator;
		this.andValue = (Number) andValue;
		this.rightValue = (Number) rightValue;
		if (node != null) exp = new Expression(node);
		
		optimize();
	}
	
	/**
	 * Constructs a AndFilter for a row-stored table
	 * @param columnName
	 * @param priority
	 * @param operator
	 * @param andValue
	 * @param rightValue
	 */
	public AndFilter(String columnName, int priority, int operator,  Object andValue, Object rightValue) {
		this.columnName = columnName;
		this.priority = priority;
		this.operator = operator;
		this.andValue = (Number) andValue;
		this.rightValue = (Number) rightValue;
		
		optimize();
	}
	
	private void optimize() {
		if (!(andValue instanceof BigDecimal) && !(andValue instanceof BigInteger) && 
				!(rightValue instanceof BigDecimal) && !(rightValue instanceof BigInteger) && 
				andValue.longValue() == rightValue.longValue()) {
			longValue = andValue.longValue();
		}
	}
	
	/**
	 * Returns {@code true} if the value matchs the filter
	 * @param value
	 */
	public boolean match(Object value) {
		if (longValue != 0) {
			long result = ((Number)value).longValue() & longValue;
			switch (operator) {
			case EQUAL:
				return result == longValue;
			case NOT_EQUAL:
				return result != longValue;
			case GREATER:
				return result > longValue;
			case GREATER_EQUAL:
				return result >= longValue;
			case LESS:
				return result < longValue;
			default: //LESS_EQUAL:
				return result <= longValue;
			}
		} else {
			Number val = Variant.and((Number)value, andValue);
			switch (operator) {
			case EQUAL:
				return Variant.isEquals(val, rightValue);
			case GREATER:
				return Variant.compare(val, rightValue, true) > 0;
			case GREATER_EQUAL:
				return Variant.compare(val, rightValue, true) >= 0;
			case LESS:
				return Variant.compare(val, rightValue, true) < 0;
			case LESS_EQUAL:
				return Variant.compare(val, rightValue, true) <= 0;
			default: //NOT_EQUAL:
				return !Variant.isEquals(val, rightValue);
			}
		}
	}

	/**
	 * 根据一块数据的最小值和最大值判断这块数据里是否可能有符合filter的数据
	 * 注意返回true也只是表示可能有符合的数据
	 */
	public boolean match(Object minValue, Object maxValue) {
		return true;
	}
}
