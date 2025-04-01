package com.scudata.dm.comparator;

import java.util.Comparator;

import com.scudata.common.ICloneable;

/**
 * 降序排序比较器
 * @author WangXiaoJun
 *
 */
public class DescComparator implements Comparator<Object>, ICloneable {
	private Comparator<Object> comparator;

	public DescComparator(Comparator<Object> comparator) {
		this.comparator = comparator;
	}

	public DescComparator() {
		this.comparator = new BaseComparator();
	}
	
	public Object deepClone() {
		if (comparator instanceof ICloneable) {
			return new DescComparator((Comparator<Object>)((ICloneable)comparator).deepClone());
		} else {
			return new DescComparator(comparator);
		}
	}

	public int compare(Object o1, Object o2) {
		return comparator.compare(o2, o1);
	}
}
