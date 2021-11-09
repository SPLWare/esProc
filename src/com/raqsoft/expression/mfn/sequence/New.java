package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.SequenceFunction;

/**
 * 针对序列做计算产生一个新序表
 * A.new(xi:Fi,…)
 * @author RunQian
 *
 */
public class New extends SequenceFunction {
	public Object calculate(Context ctx) {
		ParamInfo2 pi = ParamInfo2.parse(param, "new", false, false);
		Expression []exps = pi.getExpressions1();
		String []names = pi.getExpressionStrs2();
		return srcSequence.newTable(names, exps, option, ctx);
	}
}