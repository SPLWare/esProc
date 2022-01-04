package com.scudata.dm.comparator;

import java.util.Comparator;

import com.scudata.util.Variant;

/**
 * 使用指定语言比较器进行比较的数组比较器，null当做最小处理
 * @author WangXiaoJun
 *
 */
public class LocaleArrayComparator implements Comparator<Object> {
	private Comparator<Object> locCmp;
	private final int len;
	
	public LocaleArrayComparator(Comparator<Object> locCmp, int len) {
		this.locCmp = locCmp;
		this.len = len;
	}
	
	public int compare(Object o1, Object o2) {
		return Variant.compareArrays((Object[])o1, (Object[])o2, len, locCmp);
	}
}
