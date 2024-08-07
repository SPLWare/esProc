package com.scudata.dw;

import java.util.List;

import com.scudata.array.IArray;
import com.scudata.dm.Context;
import com.scudata.expression.Expression;

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
	
	public int colCount = 1; //filter涉及的列的个数
	protected Expression exp;//filter对应的表达式
	protected List<ColumnMetaData> columns;//有的filter对应多个列
	
	protected IArray dictMatchResult;
	
	public IFilter() {
		
	}
	
	public IFilter(ColumnMetaData column, int priority) {
		this.column = column;
		this.priority = priority;
		columnName = column.getColName();
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
	
	public String getColumnName() {
		if (columnName != null) {
			return columnName;
		}
		return column.getColName();
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
	
	public int getColCount() {
		return colCount;
	}

	public void setColCount(int colCount) {
		this.colCount = colCount;
	}
	
	public IArray calculateAll(Context ctx) {
		return exp.calculateAll(ctx);
	}
	
	public IArray calculateAnd(Context ctx, IArray leftResult) {
		return exp.calculateAnd(ctx, leftResult);
	}
	
	public int isValueRangeMatch(Context ctx) {
		return exp.isValueRangeMatch(ctx);
	}
	
	public List<ColumnMetaData> getColumns() {
		return columns;
	}

	public void setColumns(List<ColumnMetaData> columns) {
		this.columns = columns;
	}
	
	public void initExp() {
	}
	
	public Expression getExp() {
		return exp;
	}
	
	public void deepOptimize(Context ctx) {
		if (exp != null) {
			exp.deepOptimize(ctx);
		}
	}
	
	public IArray getDictMatchResult() {
		return dictMatchResult;
	}

	public void setDictMatchResult(IArray dictMatchResult) {
		this.dictMatchResult = dictMatchResult;
	}

	//是否可以跳行
	public boolean canSkipRow() {
		return dictMatchResult != null;
	}
}
