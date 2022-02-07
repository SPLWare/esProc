package com.scudata.expression.mfn.serial;

import com.scudata.common.ObjectCache;
import com.scudata.dm.Context;
import com.scudata.expression.SerialFunction;

/**
 * 取排号的长度
 * k.len()
 * @author RunQian
 *
 */
public class Len extends SerialFunction {
	public Object calculate(Context ctx) {
		return ObjectCache.getInteger(sb.length());
	}
}
