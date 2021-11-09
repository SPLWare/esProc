package com.raqsoft.dw;

/**
 * 字段过滤表达式
 * @author runqian
 *
 */
public abstract class IFilter implements Comparable<IFilter> {
	public static final int EQUAL = 1;
	public static final int GREATER = 2;
	public static final int GREATER_EQUAL = 3;
	public static final int LESS  = 4;
	public static final int LESS_EQUAL = 5;
	public static final int NOT_EQUAL = 6;
	
	public static final int AND = 10;
	public static final int OR = 11;
	
	protected ColumnMetaData column;
	protected int priority; // 优先级，用于排序，先过滤优先级高的字段，数字越小优先级越高
	protected String columnName;//用于行存
	
	public IFilter() {
		
	}
	
	public IFilter(ColumnMetaData column, int priority) {
		this.column = column;
		this.priority = priority;
	}
	
	/**
	 * 取运算符的相反运算符，用于 v ? f时的转换，此时字段在右
	 * @param op
	 * @return
	 */
	public static int getInverseOP(int op) {
		switch (op) {
		case GREATER:
			return LESS;
		case GREATER_EQUAL:
			return LESS_EQUAL;
		case LESS:
			return GREATER;
		case LESS_EQUAL:
			return GREATER_EQUAL;
		default:
			return op;
		}
	}

	public ColumnMetaData getColumn() {
		return column;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public boolean isSameColumn(IFilter other) {
		if (columnName != null) {
			return columnName.equals(other.columnName);
		}
		return column == other.column;
	}
	
	/**
	 * 返回value是否匹配此过滤表达式
	 * @param value
	 * @return
	 */
	public abstract boolean match(Object value);
	
	/**
	 * 返回区间[minValue, maxValue]是否有值匹配此过滤表达式
	 * @param minValue
	 * @param maxValue
	 * @return
	 */
	public abstract boolean match(Object minValue, Object maxValue);
	
	public int compareTo(IFilter o) {
		if (priority < o.priority) {
			return -1;
		} else if (priority > o.priority) {
			return 1;
		} else {
			return 0;
		}
	}
	
	/**
	 * 是否是多字段or
	 * @return
	 */
	public boolean isMultiFieldOr() {
		return false;
	}
}
