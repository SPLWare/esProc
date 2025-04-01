package com.scudata.dw;

import com.scudata.array.IArray;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.util.Variant;

/**
 * 包含过滤器类
 * 用于判断一个对象是否包含在给定的序列里
 * @author runqian
 *
 */
public class ContainFilter extends IFilter {
	public static final int BINARYSEARCH_COUNT = 3; // 元素个数大于此值采用二分法查找
	private IArray values;
	/**
	 * 构造器
	 * @param column 列对象
	 * @param priority 优先级
	 * @param sequence 给定的序列
	 * @param opt 等于b时表示sequence已经有序
	 */
	public ContainFilter(ColumnMetaData column, int priority, Sequence sequence, String opt) {
		super(column, priority);
		values = sequence.getMems();
		if (opt == null || opt.indexOf('b') == -1) {
			values.sort();
		}
	}
	
	public ContainFilter(ColumnMetaData column, int priority, Sequence sequence, String opt, Node node) {
		super(column, priority);
		values = sequence.getMems();
		if (opt == null || opt.indexOf('b') == -1) {
			values.sort();
		}
		if (node != null) exp = new Expression(node);
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
		values = sequence.getMems();
		if (opt == null || opt.indexOf('b') == -1) {
			values.sort();
		}
	}
	
	public boolean match(Object value) {
		return values.binarySearch(value) > 0;
	}
	
	public boolean match(Object minValue, Object maxValue) {
		IArray values = this.values;
		int len = values.size();
		
		// 二分法查找最小值在数组中的位置
		int low1 = 1;
		int high1 = len;
		while (low1 <= high1) {
			int mid = (low1 + high1) >>> 1;
			int cmp = Variant.compare(values.get(mid), minValue, true);
			
			if (cmp < 0)
				low1 = mid + 1;
			else if (cmp > 0)
				high1 = mid - 1;
			else
				return true; // key found
		}
		
		// 块最小值比集合最大值大或者块最小值等于最大值则没有符合条件的记录
		if (low1 > len || Variant.isEquals(minValue, maxValue)) {
			return false;
		}
		
		// 二分法查找最大值在数组中的位置
		int low2 = low1;
		int high2 = len;
		while (low2 <= high2) {
			int mid = (low2 + high2) >>> 1;
			int cmp = Variant.compare(values.get(mid), maxValue, true);
			
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