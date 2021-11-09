package com.raqsoft.dm.comparator;

import java.util.Comparator;
import com.raqsoft.util.Variant;

// Env.Locale对应的比较器，缺省比较字符串的unicode值
public class LocaleComparator implements Comparator<Object> {
	private Comparator<Object> locCmp;
	private boolean throwExcept = true;

	public LocaleComparator(Comparator<Object> locCmp) {
		this.locCmp = locCmp;
	}

	public LocaleComparator(Comparator<Object> locCmp, boolean throwExcept) {
		this.locCmp = locCmp;
		this.throwExcept = throwExcept;
	}

	public int compare(Object o1, Object o2) {
		return Variant.compare(o1, o2, locCmp, throwExcept);
	}
}
