package com.scudata.dm.comparator;

import java.util.Comparator;

import com.scudata.util.Variant;

/**
 * null当做最大处理的比较器
 * @author WangXiaoJun
 *
 */
public class BaseComparator_0 implements Comparator<Object> {
	public int compare(Object o1, Object o2) {
		return Variant.compare_0(o1, o2);
	}
}
