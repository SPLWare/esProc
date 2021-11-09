package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.SequenceFunction;

/**
 * 计算满足给定条件的元素取值为真（非空并且不是false）的个数
 * A.countif(Ai:xi,…)
 * @author RunQian
 *
 */
public class Countif extends SequenceFunction {
	public Object calculate(Context ctx) {
		return Sumif.posSelect("countif", srcSequence, param, option, ctx).count(option);
	}
}
