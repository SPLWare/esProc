package com.scudata.expression.mfn.sequence;

import com.scudata.dm.Context;
import com.scudata.expression.Expression;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;

/**
 * 取序列中满足指定条件的元素生成新的序列
 * A.select(x) A.select(xk:yk,…)
 * @author RunQian
 *
 */
public class Select extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.select(null, option, ctx);
		} else if (param.isLeaf()) {
			return srcSequence.select(param.getLeafExpression(), option, ctx);
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "select", true, true);
			Expression[] fltExps = pi.getExpressions1();
			Object[] vals = pi.getValues2(ctx);
			return srcSequence.select(fltExps, vals, option, ctx);
		}
	}
}
