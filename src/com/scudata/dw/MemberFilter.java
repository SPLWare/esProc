package com.scudata.dw;

import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

class MemberFilter extends FindFilter {
	private Sequence sequence;
	private static final int MAX_CHECK_NUMBER = 500;
	public MemberFilter(ColumnMetaData column, int priority, Sequence sequence) {
		super(column, priority);
		this.sequence = sequence;
	}
	
	public MemberFilter(String columnName, int priority, Sequence sequence) {
		super(columnName, priority);
		this.sequence = sequence;
	}
	
	public boolean match(Object value) {
		try {
			return Variant.isTrue(sequence.get(((Number)value).intValue()));
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean match(Object minValue, Object maxValue) {
		int min;
		int max;
		try {
			min = ((Number)minValue).intValue();
			max = ((Number)maxValue).intValue();
		} catch (Exception e) {
			return false;
		}
		
		if (max > min && max - min <= MAX_CHECK_NUMBER) {
			Sequence sequence = this.sequence;
			for (int i = min; i <= max; i++) {
				if (Variant.isTrue(sequence.get(i))) {
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}
	
	public Object getFindResult() {
		return Boolean.TRUE;//能找到时，A(K)一定是true
	}
}