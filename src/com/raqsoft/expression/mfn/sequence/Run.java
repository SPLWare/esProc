package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.SequenceFunction;

/**
 * 针对序列计算表达式，返回序列本身
 * A.run(xi,…) P.run(xi:Fi:,…)
 * @author RunQian
 *
 */
public class Run extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence;
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			srcSequence.run(exp, option, ctx);
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "run", true, false);
			Expression []exps = pi.getExpressions1();
			Expression []assignExps = pi.getExpressions2();
			srcSequence.run(assignExps, exps, ctx);
		}

		return srcSequence;
	}
}
