package com.raqsoft.dm.comparator;

import java.util.Comparator;

/**
 * PSortItem对象比较器，用于返回位置的排序
 * @author WangXiaoJun
 *
 */
public class PSortComparator implements Comparator<Object> {
	private Comparator<Object> comparator;

	/**
	 * 用指定比较起进行排序
	 * @param comparator Comparator 不可为空
	 */
	public PSortComparator(Comparator<Object> comparator) {
		this.comparator = comparator;
	}

	public int compare(Object o1, Object o2) {
		return comparator.compare(((PSortItem)o1).value, ((PSortItem)o2).value);
	}
}
