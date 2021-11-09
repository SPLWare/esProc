package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.SequenceFunction;

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
