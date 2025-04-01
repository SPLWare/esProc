package com.scudata.vdb;

import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

/**
 * 目录过滤器
 * @author RunQian
 *
 */
class DirFilter {
	private Object rightValue;
	private Sequence values; // 序列表示从属关系
	private boolean valueSign; // true：对目录提条件，此时如果传入的目录值是null，则会选值是null的目录，false：省略目录值，即不对此目录提条件

	public DirFilter(Object rightValue, boolean valueSign) {
		this.rightValue = rightValue;
		this.valueSign = valueSign;
		if (rightValue instanceof Sequence) {
			values = (Sequence)rightValue;
		}
	}
	
	public boolean match(Object value) {
		if (values == null) {
			if (valueSign) {
				return Variant.isEquals(value, rightValue);
			} else {
				return true;
			}
		} else {
			return values.contains(value, false);
		}
	}
}