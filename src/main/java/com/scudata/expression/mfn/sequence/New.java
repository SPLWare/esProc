package com.scudata.expression.mfn.sequence;

import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;

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