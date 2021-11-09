package com.raqsoft.expression.mfn;

import com.raqsoft.dm.Context;
import com.raqsoft.dm.Record;
import com.raqsoft.expression.MemberFunction;

/**
 * 返回记录的键，如果没有主键则返回所有字段组成的序列，如果不是记录则返回本身
 * v.v()
 * @author RunQian
 *
 */
public class Value extends MemberFunction {
	protected Object src;
	
	public boolean isLeftTypeMatch(Object obj) {
		return true;
	}

	public void setDotLeftObject(Object obj) {
		src = obj;
	}

	public Object calculate(Context ctx) {
		if (src instanceof Record) {
			return ((Record)src).value();
		} else {
			return src;
		}
	}
}
