package com.raqsoft.dm.comparator;

import java.util.Comparator;
import com.raqsoft.util.Variant;

// Env.Locale对应的比较器，缺省比较字符串的unicode值
public class LocaleComparator_0 implements Comparator<Object> {
	private Comparator<Object> locCmp;

	public LocaleComparator_0(Comparator<Object> locCmp) {
		this.locCmp = locCmp;
	}

	public int compare(Object o1, Object o2) {
		return Variant.compare_0(o1, o2, locCmp);
	}
}
