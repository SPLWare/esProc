package com.scudata.dw;

import com.scudata.dm.Sequence;
import com.scudata.thread.MultithreadUtil;
import com.scudata.util.Variant;

/**
 * 不包含过滤器类
 * 与ContainFilter的逻辑相反，用于判断一个对象是否不包含在给定的序列里
 * @author runqian
 *
 */
public class NotContainFilter extends IFilter {
	private Object []values;

	public NotContainFilter(ColumnMetaData column, int priority, Sequence sequence, String opt) {
		super(column, priority);
		values = sequence.toArray();
		if (opt == null || opt.indexOf('b') == -1) {
			MultithreadUtil.sort(values);
		}
	}

	public NotContainFilter(String columnName, int priority, Sequence sequence, String opt) {
		this.columnName = columnName;
		this.priority = priority;
		values = sequence.toArray();
		if (opt == null || opt.indexOf('b') == -1) {
			MultithreadUtil.sort(values);
		}
	}
	
	public boolean match(Object value) {
		Object []values = this.values;
		int len = values.length;
		if (len > ContainFilter.BINARYSEARCH_COUNT) {
			// 二分法查找
			int low = 0;
			int high = len - 1;

			while (low <= high) {
				int mid = (low + high) >>> 1;
				int cmp = Variant.compare(values[mid], value, true);
				
				if (cmp < 0)
					low = mid + 1;
				else if (cmp > 0)
					high = mid - 1;
				else
					return false; // key found
			}
		} else {
			for (Object v : values) {
				if (Variant.isEquals(value, v)) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public boolean match(Object minValue, Object maxValue) {
		if (Variant.isEquals(minValue, maxValue)) {
			return match(minValue);
		} else {
			return true;
		}
	}
}