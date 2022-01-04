package com.scudata.expression.mfn.sequence;

import com.scudata.dm.Context;
import com.scudata.expression.SequenceFunction;

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
