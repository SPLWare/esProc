package com.raqsoft.dw;

import java.util.Comparator;

import com.raqsoft.dm.comparator.BaseComparator;
import com.raqsoft.dm.comparator.DescComparator;
import com.raqsoft.expression.fn.gather.Top;
import com.raqsoft.util.MinHeap;

/**
 * Top表达式的过滤器
 * @author runqian
 *
 */
public class TopFilter extends IFilter {
	private Top topNode;
	Comparator<Object> comparator;
	MinHeap heap;
	
	public TopFilter(ColumnMetaData column, int priority, Top topNode) {
		super(column, priority);
		this.column = column;
		this.priority = priority;
		this.topNode = topNode;
		init();
	}
	
	private void init() {
		comparator = new BaseComparator();
		if (!topNode.isPositive()) {
			comparator = new DescComparator(comparator);
		}
		heap = new MinHeap(topNode.getCount(), comparator);
	}
	
	public boolean match(Object value) {
		return heap.insert(value);
	}

	public boolean match(Object minValue, Object maxValue) {
		Object obj = heap.getTop();
		
		boolean min, max;
		if (comparator.compare(minValue, obj) >= 0) {
			min = false;
		} else {
			min = true;
		}
		if (comparator.compare(maxValue, obj) >= 0) {
			max = false;
		} else {
			max = true;
		}
		
		return min || max;
	}
}
