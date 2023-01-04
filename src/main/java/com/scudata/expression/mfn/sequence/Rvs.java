package com.scudata.expression.mfn.sequence;

import com.scudata.dm.Context;
import com.scudata.expression.SequenceFunction;

/**
 * 倒转序列的元素组成新元素
 * A.rvs()
 * @author RunQian
 *
 */
public class Rvs extends SequenceFunction {
	public Object calculate(Context ctx) {
		return srcSequence.rvs();
	}
}