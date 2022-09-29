package com.scudata.dw;

import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

public class MemberFilter extends FindFilter {
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
			int n = ((Number)value).intValue();
			if (n < 1 || n > sequence.length()) {
				findResult = null;
				return false;
			} else {
				findResult = sequence.getMem(n);
				return Variant.isTrue(findResult);
			}
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
		
		Sequence sequence = this.sequence;
		int len = sequence.length();
		if (min > len) {
			return false;
		}
		
		if (max > len) {
			max = len;
		}
		
		if (max - min <= MAX_CHECK_NUMBER) {
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
		//return Boolean.TRUE;//能找到时，A(K)一定是true
		return findResult;
	}
}