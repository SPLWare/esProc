package com.scudata.dm.comparator;

import java.util.Comparator;

import com.scudata.common.ICloneable;

/**
 * 数组比较器，null当做最小处理
 * @author WangXiaoJun
 *
 */
public class ArrayComparator2 implements Comparator<Object>, ICloneable {
	private final CommonComparator []comparators;
	private final int len;
	
	public ArrayComparator2(CommonComparator []comparators, int len) {
		this.comparators = comparators;
		this.len = len;
	}

	public Object deepClone() {
		CommonComparator []cmps = new CommonComparator[len];
		for (int i = 0; i < len; ++i) {
			cmps[i] = (CommonComparator)comparators[i].deepClone();
		}
		
		return new ArrayComparator2(cmps, len);
	}
	
	public int compare(Object o1, Object o2) {
		Object []a1 = (Object[])o1;
		Object []a2 = (Object[])o2;
		
		for (int i = 0; i < len; ++i) {
			int cmp = comparators[i].compare(a1[i], a2[i]);
			if (cmp != 0) {
				return cmp;
			}
		}
		
		return 0;
	}
}
