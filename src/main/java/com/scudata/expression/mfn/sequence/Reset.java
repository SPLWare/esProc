package com.scudata.expression.mfn.sequence;

import com.scudata.dm.Context;
import com.scudata.expression.SequenceFunction;

/**
 * 清空序列的元素
 * A.reset()
 * @author RunQian
 *
 */
public class Reset extends SequenceFunction {
	public Object calculate(Context ctx) {
		srcSequence.reset();
		return srcSequence;
	}
}
