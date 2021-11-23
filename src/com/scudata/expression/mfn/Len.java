package com.scudata.expression.mfn;

import com.scudata.common.ObjectCache;
import com.scudata.dm.Context;
import com.scudata.dm.SerialBytes;
import com.scudata.expression.MemberFunction;

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
