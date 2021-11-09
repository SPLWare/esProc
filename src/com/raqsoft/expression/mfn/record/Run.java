package com.raqsoft.expression.mfn.record;

import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.RecordFunction;

/**
 * 针对记录计算表达式，返回记录本身
 * r.run(xi,…) r.run(xi:Fi:,…)
 * @author RunQian
 *
 */
public class Run extends RecordFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcRecord;
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			srcRecord.run(exp, ctx);
		} else {
			ParamInfo2 pi = ParamInfo2.parse(param, "run", true, false);
			Expression []exps = pi.getExpressions1();
			Expression []assignExps = pi.getExpressions2();
			srcRecord.run(assignExps, exps, ctx);
		}

		return srcRecord;
	}
}
