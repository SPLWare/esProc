package com.scudata.dw;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.dm.Context;
import com.scudata.expression.Relation;

public class ColumnOr extends IFilter {
	private ColumnsOr or;
	private int i;
	private boolean lastCol = false;

	IFilter filter;
	
	public ColumnOr(ColumnsOr or, IFilter filter) {
		super(filter.column, filter.priority);
		this.or = or;
		this.filter = filter;
		this.columnName = filter.columnName;
	}
	
	public void setI(int i) {
		this.i = i;
	}
	
	public void setLastCol(boolean lastCol) {
		this.lastCol = lastCol;
	}
	
	public boolean match(Object value) {
		if (i == 0) {
			or.sign = filter.match(value);
			return true;
		} else {
			if (or.sign) {
				return true;
			} else {
				if (filter.match(value)) {
					or.sign = true;
					return true;
				} else {
					if (lastCol) {
						return false;
					} else {
						return true;
					}
				}
			}
		}
	}
	
	public boolean match(Object minValue, Object maxValue) {
		if (i == 0) {
			or.sign = filter.match(minValue, maxValue);
			return true;
		} else {
			if (or.sign) {
				return true;
			} else {
				if (filter.match(minValue, maxValue)) {
					or.sign = true;
					return true;
				} else {
					if (lastCol) {
						return false;
					} else {
						return true;
					}
				}
			}
		}
	}
	
	// 是否是多字段or
	public boolean isMultiFieldOr() {
		return true;
	}
	
	public IArray calculateAll(Context ctx) {
		if (i == 0) {
			or.signArray = (BoolArray) filter.calculateAll(ctx);
			return new BoolArray(true, or.signArray.size());//此时必须返回全true
		} else {
			BoolArray signArray = or.signArray;
			int size = signArray.size();
			boolean hasFalse = false;
			for (int i = 1; i <= size; i++) {
				if (signArray.isFalse(i)) {
					hasFalse = true;
					break;
				}
			}
			
			if (hasFalse) {
				BoolArray newSignArray = (BoolArray) filter.calculateAll(ctx);
				or.signArray = signArray.calcRelation(newSignArray, Relation.OR);
				if (lastCol) {
					return or.signArray;
				} else {
					return new BoolArray(true, or.signArray.size());//此时必须返回全true
				}
			} else {
				return signArray;
			}
		}
	}
	
	public IArray calculateAnd(Context ctx, IArray leftResult) {
		return calculateAll(ctx);//ColumnsOr时，不是与的关系
	}
	
	public int isValueRangeMatch(Context ctx) {
		if (i == 0) {
			or.sign = filter.isValueRangeMatch(ctx) >= 0;
			return 0;
		} else {
			if (or.sign) {
				return 0;
			} else {
				if (filter.isValueRangeMatch(ctx) >= 0) {
					or.sign = true;
					return 0;
				} else {
					if (lastCol) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		}
	}
	
	public void deepOptimize(Context ctx) {
		filter.deepOptimize(ctx);
	}
}