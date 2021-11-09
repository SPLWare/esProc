package com.raqsoft.expression.mfn;

import com.raqsoft.common.ObjectCache;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.SerialBytes;
import com.raqsoft.expression.MemberFunction;

/**
 * 取排号的长度
 * k.len()
 * @author RunQian
 *
 */
public class Len extends MemberFunction {
	protected SerialBytes sb; // 排号

	public boolean isLeftTypeMatch(Object obj) {
		return obj instanceof SerialBytes;
	}
	
	public void setDotLeftObject(Object obj) {
		sb = (SerialBytes)obj;
	}

	public Object calculate(Context ctx) {
		return ObjectCache.getInteger(sb.length());
	}
}
