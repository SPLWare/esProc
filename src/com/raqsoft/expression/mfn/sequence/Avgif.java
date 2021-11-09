package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.SequenceFunction;

/**
 * 计算满足给定条件的元素的平均值
 * A.avgif(Ai:xi,…)
 * @author RunQian
 *
 */
public class Avgif extends SequenceFunction {
	public Object calculate(Context ctx) {
		return Sumif.posSelect("avgif", srcSequence, param, option, ctx).average();
	}
}