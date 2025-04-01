package com.scudata.dm.comparator;

import java.util.Comparator;

import com.scudata.util.Variant;

/**
 * 数组比较器，null当做最小处理
 * @author WangXiaoJun
 *
 */
public class ArrayComparator implements Comparator<Object> {
	private final int len;
	
	public ArrayComparator(int len) {
		this.len = len;
	}

	public int compare(Object o1, Object o2) {
		return Variant.compareArrays((Object[])o1, (Object[])o2, len);
	}
}
