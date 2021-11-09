package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.SequenceFunction;

/**
 * 返回序列的长度
 * A.len()
 * @author RunQian
 *
 */
public class Len extends SequenceFunction {
	public Object calculate(Context ctx) {
		return srcSequence.length();
	}
}
