package com.scudata.expression.mfn.sequence;

import com.scudata.dm.Context;
import com.scudata.expression.SequenceFunction;

/**
 * 计算序列中满足给定条件的元素的最小值
 * A.minif(Ai:xi,…)
 * @author RunQian
 *
 */
public class Minif extends SequenceFunction {
	public Object calculate(Context ctx) {
		return Sumif.posSelect("minif", srcSequence, param, option, ctx).min();
	}
}
