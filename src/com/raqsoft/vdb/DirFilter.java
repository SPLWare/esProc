package com.raqsoft.vdb;

import com.raqsoft.dm.Sequence;
import com.raqsoft.util.Variant;

/**
 * 目录过滤器
 * @author RunQian
 *
 */
class DirFilter {
	private Object rightValue;
	private Sequence values; // 序列表示从属关系

	public DirFilter(Object rightValue) {
		this.rightValue = rightValue;
		if (rightValue instanceof Sequence) {
			values = (Sequence)rightValue;
		}
	}
	
	public boolean match(Object value) {
		if (values == null) {
			if (rightValue != null) {
				return Variant.isEquals(value, rightValue);
			} else {
				return true;
			}
		} else {
			return values.contains(value, false);
		}
	}
}