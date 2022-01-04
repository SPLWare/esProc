package com.scudata.dw;

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
	
	// ÊÇ·ñÊÇ¶à×Ö¶Îor
	public boolean isMultiFieldOr() {
		return true;
	}
}