package com.raqsoft.dw;

import com.raqsoft.dm.Sequence;
import com.raqsoft.thread.MultithreadUtil;
import com.raqsoft.util.Variant;

/**
 * 包含过滤器类
 * 用于判断一个对象是否包含在给定的序列里
 * @author runqian
 *
 */
class ContainFilter extends IFilter {
	public static final int BINARYSEARCH_COUNT = 3; // 元素个数大于此值采用二分法查找
	private Object []values;

	/**
	 * 构造器
	 * @param column 列对象
	 * @param priority 优先级
	 * @param sequence 给定的序列
	 * @param opt 等于b时表示sequence已经有序
	 */
	public ContainFilter(ColumnMetaData column, int priority, Sequence sequence, String opt) {
		super(column, priority);
		values = sequence.toArray();
		if (opt == null || opt.indexOf('b') == -1) {
			MultithreadUtil.sort(values);
		}
	}
	
	/**
	 * 构造器 (用于行存)
	 * @param columnName 列名称
	 * @param priority 优先级
	 * @param sequence 给定的序列
	 * @param opt 等于b时表示sequence已经有序
	 */
	public ContainFilter(String columnName, int priority, Sequence sequence, String opt) {
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
		if (len > BINARYSEARCH_COUNT) {
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
					return true; // key found
			}
		} else {
			for (Object v : values) {
				if (Variant.isEquals(value, v)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean match(Object minValue, Object maxValue) {
		Object []values = this.values;
		int len = values.length;
		
		// 二分法查找最小值在数组中的位置
		int low1 = 0;
		int high1 = len - 1;
		while (low1 <= high1) {
			int mid = (low1 + high1) >>> 1;
			int cmp = Variant.compare(values[mid], minValue, true);
			
			if (cmp < 0)
				low1 = mid + 1;
			else if (cmp > 0)
				high1 = mid - 1;
			else
				return true; // key found
		}
		
		// 块最小值比集合最大值大或者块最小值等于最大值则没有符合条件的记录
		if (low1 >= len || Variant.isEquals(minValue, maxValue)) {
			return false;
		}
		
		// 二分法查找最大值在数组中的位置
		int low2 = 0;
		int high2 = len - 1;
		while (low2 <= high2) {
			int mid = (low2 + high2) >>> 1;
			int cmp = Variant.compare(values[mid], maxValue, true);
			
			if (cmp < 0)
				low2 = mid + 1;
			else if (cmp > 0)
				high2 = mid - 1;
			else
				return true; // key found
		}
		
		// 如果块最小值和块最大值在集合中的插入位置相同则没有符合条件的记录
		return low1 != low2;
	}
}